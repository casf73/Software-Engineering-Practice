package cn.codelyy.store.domain;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="customer_accounts",uniqueConstraints=@UniqueConstraint(name="uk_customer_username_key",columnNames="username_key"))
public class Customer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=32) private String username;
    @Column(name="username_key",nullable=false,length=32) private String usernameKey;
    @Column(name="password_hash",nullable=false,length=256) private String passwordHash;
    @Column(nullable=false,length=30) private String phone;
    @Column(name="default_location",nullable=false,length=200) private String defaultLocation;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getUsernameKey(){return usernameKey;} public void setUsernameKey(String v){usernameKey=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getDefaultLocation(){return defaultLocation;} public void setDefaultLocation(String v){defaultLocation=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
