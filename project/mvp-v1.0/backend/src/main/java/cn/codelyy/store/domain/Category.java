package cn.codelyy.store.domain;
import jakarta.persistence.*;
@Entity @Table(name="categories")
public class Category {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=80) private String name;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id") private Category parent;
 public Long getId(){return id;} public String getName(){return name;}
 public void setName(String v){name=v;} public Category getParent(){return parent;}
 public void setParent(Category v){parent=v;}
}
