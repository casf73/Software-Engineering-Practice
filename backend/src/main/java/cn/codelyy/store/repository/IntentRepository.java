package cn.codelyy.store.repository;

import cn.codelyy.store.domain.IntentStatus;
import cn.codelyy.store.domain.PurchaseIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IntentRepository extends JpaRepository<PurchaseIntent, Long> {
    Optional<PurchaseIntent> findByPasscode(String passcode);
    List<PurchaseIntent> findByProductIdOrderByQueueAtAscIdAsc(Long productId);
    List<PurchaseIntent> findByProductIdAndStatusOrderByQueueAtAscIdAsc(Long productId, IntentStatus status);
    Optional<PurchaseIntent> findFirstByProductIdAndStatusOrderByQueueAtAscIdAsc(Long productId, IntentStatus status);
}
