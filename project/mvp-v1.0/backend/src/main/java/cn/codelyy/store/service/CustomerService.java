package cn.codelyy.store.service;
import cn.codelyy.store.api.ApiException;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class CustomerService {
    public static final String SESSION_KEY="customer-account-id";
    private final CustomerRepository customers;
    private final IntentRepository intents;
    public CustomerService(CustomerRepository customers,IntentRepository intents){this.customers=customers;this.intents=intents;}
    @Transactional
    public CustomerView register(CustomerRegisterRequest request){
        String key=request.username().trim().toLowerCase(Locale.ROOT);
        if(customers.existsByUsernameKey(key))throw ApiException.conflict("用户名已被注册");
        Customer customer=new Customer();
        customer.setUsername(request.username().trim());customer.setUsernameKey(key);
        customer.setPasswordHash(PasswordHasher.hash(request.password()));
        customer.setPhone(request.phone().trim());customer.setDefaultLocation(request.defaultLocation().trim());
        return view(customers.saveAndFlush(customer));
    }
    @Transactional(readOnly=true)
    public CustomerView login(CustomerLoginRequest request){
        Customer customer=customers.findByUsernameKey(request.username().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"用户名或密码错误"));
        if(!PasswordHasher.matches(request.password(),customer.getPasswordHash()))throw new ApiException(HttpStatus.UNAUTHORIZED,"用户名或密码错误");
        return view(customer);
    }
    @Transactional(readOnly=true)
    public Customer requireCustomer(HttpServletRequest request){
        var session=request.getSession(false);
        if(session==null||!(session.getAttribute(SESSION_KEY) instanceof Long id))throw new ApiException(HttpStatus.UNAUTHORIZED,"请先登录客户账号");
        return customers.findById(id).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"客户登录已失效，请重新登录"));
    }
    @Transactional(readOnly=true)
    public CustomerSessionView session(HttpServletRequest request){
        var session=request.getSession(false);
        if(session==null||!(session.getAttribute(SESSION_KEY) instanceof Long id))return new CustomerSessionView(false,null);
        return customers.findById(id).map(c->new CustomerSessionView(true,view(c))).orElseGet(()->new CustomerSessionView(false,null));
    }
    @Transactional(readOnly=true)
    public PageView<CustomerView> list(String query,int page,int size){
        String keyword=query==null?"":query.trim();
        var data=customers.findByUsernameContainingIgnoreCaseOrPhoneContaining(keyword,keyword,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"id")));
        return new PageView<>(data.getContent().stream().map(this::view).toList(),data.getTotalElements(),page,size);
    }
    @Transactional(readOnly=true)
    public CustomerView customer(Long id){return view(customers.findById(id).orElseThrow(()->ApiException.notFound("客户不存在")));}
    @Transactional(readOnly=true)
    public PageView<CustomerOrderView> orders(Long customerId,int page,int size,boolean includePasscode){
        if(!customers.existsById(customerId))throw ApiException.notFound("客户不存在");
        var data=intents.findByCustomerIdOrderBySubmittedAtDescIdDesc(customerId,PageRequest.of(page,size));
        return new PageView<>(data.getContent().stream().map(i->orderView(i,includePasscode)).toList(),data.getTotalElements(),page,size);
    }
    private CustomerOrderView orderView(PurchaseIntent intent,boolean includePasscode){
        Product product=intent.getProduct();Integer position=null;
        if(intent.getStatus()==IntentStatus.WAITING){
            var queue=intents.findByProductIdAndStatusOrderByQueueAtAscIdAsc(product.getId(),IntentStatus.WAITING);
            for(int i=0;i<queue.size();i++)if(Objects.equals(queue.get(i).getId(),intent.getId())){position=i+1;break;}
        }
        return new CustomerOrderView(intent.getId(),product.getId(),
            intent.getProductNameSnapshot()==null?product.getName():intent.getProductNameSnapshot(),
            intent.getPriceSnapshot()==null?product.getPrice():intent.getPriceSnapshot(),
            intent.getName(),intent.getPhone(),intent.getMeetingLocation(),intent.getStatus(),intent.getSubmittedAt(),position,
            includePasscode?intent.getPasscode():null,product.getStatus());
    }
    private CustomerView view(Customer customer){return new CustomerView(customer.getId(),customer.getUsername(),customer.getPhone(),customer.getDefaultLocation(),customer.getCreatedAt());}
}
