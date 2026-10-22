package cn.codelyy.store.repository;

import cn.codelyy.store.domain.IntentStatus;
import cn.codelyy.store.domain.PurchaseIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IntentRepository extends JpaRepository<PurchaseIntent, Long> {
    @org.springframework.data.jpa.repository.Query("select i.product.id from PurchaseIntent i where i.id = :id")
    Optional<Long> findProductIdByIntentId(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<PurchaseIntent> findByPasscode(String passcode);
    @org.springframework.data.jpa.repository.Query("select i.product.id from PurchaseIntent i where i.passcode = :passcode")
    Optional<Long> findProductIdByPasscode(@org.springframework.data.repository.query.Param("passcode") String passcode);
    List<PurchaseIntent> findByProductIdOrderByQueueAtAscIdAsc(Long productId);
    List<PurchaseIntent> findByProductIdAndStatusOrderByQueueAtAscIdAsc(Long productId, IntentStatus status);
    Optional<PurchaseIntent> findFirstByProductIdAndStatusOrderByQueueAtAscIdAsc(Long productId, IntentStatus status);
}
