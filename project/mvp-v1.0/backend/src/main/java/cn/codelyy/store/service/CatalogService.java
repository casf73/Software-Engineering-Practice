package cn.codelyy.store.service;
import cn.codelyy.store.api.*;
import cn.codelyy.store.api.ApiModels.*;
import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class CatalogService {
 private final CategoryRepository categories; private final ProductRepository products; private final SellerAccountRepository seller;
 public CatalogService(CategoryRepository c,ProductRepository p,SellerAccountRepository s){categories=c;products=p;seller=s;}
 @Transactional(readOnly=true) public List<CategoryView> list(){return categories.findAll().stream().sorted(Comparator.comparing(Category::getId)).map(this::view).toList();}
 @Transactional public CategoryView save(Long id,CategoryRequest r){
  seller.findLockedById(1L).orElseThrow();
  Category c=id==null?new Category():categories.findById(id).orElseThrow(()->ApiException.notFound("分类不存在"));
  Category parent=r.parentId()==null?null:categories.findById(r.parentId()).orElseThrow(()->ApiException.notFound("父分类不存在"));
  if(parent!=null && (parent.getParent()!=null || Objects.equals(parent.getId(),id)))throw ApiException.badRequest("只支持两级分类，不能循环或指定二级分类为父分类");
  if(id!=null && parent!=null && categories.existsByParentId(id))throw ApiException.conflict("包含子分类的一级分类不能降为二级");
  String name=r.name().trim();
  if(categories.findAll().stream().anyMatch(x->!Objects.equals(x.getId(),id)&&x.getName().equalsIgnoreCase(name)&&Objects.equals(x.getParent()==null?null:x.getParent().getId(),r.parentId())))throw ApiException.conflict("同级分类名称重复");
  c.setName(name);c.setParent(parent);return view(categories.save(c));
 }
 @Transactional public void delete(Long id){
  seller.findLockedById(1L).orElseThrow();
  if(!categories.existsById(id))throw ApiException.notFound("分类不存在");
  if(categories.existsByParentId(id)||products.existsByCategoryId(id))throw ApiException.conflict("分类已被子分类或商品引用，请先调整关联");
  categories.deleteById(id);
 }
 private CategoryView view(Category c){return new CategoryView(c.getId(),c.getName(),c.getParent()==null?null:c.getParent().getId());}
}
