package cn.codelyy.store.repository;
import cn.codelyy.store.domain.Customer;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CustomerRepository extends JpaRepository<Customer,Long> {
    Optional<Customer> findByUsernameKey(String usernameKey);
    boolean existsByUsernameKey(String usernameKey);
    Page<Customer> findByUsernameContainingIgnoreCaseOrPhoneContaining(String username,String phone,Pageable pageable);
}
