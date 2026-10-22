package cn.codelyy.store.repository;

import cn.codelyy.store.domain.SellerAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SellerAccountRepository extends JpaRepository<SellerAccount, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SellerAccount a where a.id = :id")
    Optional<SellerAccount> findLockedById(@Param("id") Long id);
}
