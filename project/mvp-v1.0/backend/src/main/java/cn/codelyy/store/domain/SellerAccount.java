package cn.codelyy.store.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "seller_accounts")
public class SellerAccount {
    @Id private Long id = 1L;
    @Column(name = "password_hash", nullable = false, length = 256)
    private String passwordHash;
    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    public Long getId() { return id; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public long getSessionVersion() { return sessionVersion; }
    public void setSessionVersion(long value) { sessionVersion = value; }
}
