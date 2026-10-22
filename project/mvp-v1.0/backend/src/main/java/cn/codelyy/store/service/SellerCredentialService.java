package cn.codelyy.store.service;

import cn.codelyy.store.api.ApiException;
import cn.codelyy.store.api.ApiModels.ChangePasswordRequest;
import cn.codelyy.store.config.SellerProperties;
import cn.codelyy.store.domain.SellerAccount;
import cn.codelyy.store.repository.SellerAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerCredentialService implements ApplicationRunner {
    private final SellerAccountRepository accounts;
    private final SellerProperties properties;

    public SellerCredentialService(SellerAccountRepository accounts, SellerProperties properties) {
        this.accounts = accounts;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 配置密码只用于首次建库；重启或修改环境变量不会覆盖已修改的密码。
        if (!accounts.existsById(1L)) {
            SellerAccount account = new SellerAccount();
            account.setPasswordHash(PasswordHasher.hash(properties.password()));
            accounts.saveAndFlush(account);
        }
    }

    @Transactional(readOnly = true)
    public long authenticate(String username, String password) {
        SellerAccount account = account();
        if (!properties.username().equals(username) || !PasswordHasher.matches(password, account.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        return account.getSessionVersion();
    }

    @Transactional(readOnly = true)
    public boolean isCurrentSession(Object version) {
        return version instanceof Long value && value == account().getSessionVersion();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Object sessionVersion) {
        SellerAccount account = accounts.findLockedById(1L)
                .orElseThrow(() -> new IllegalStateException("卖家账号尚未初始化"));
        if (!(sessionVersion instanceof Long value) || value != account.getSessionVersion()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        if (!PasswordHasher.matches(request.currentPassword(), account.getPasswordHash())) {
            throw ApiException.badRequest("当前密码不正确");
        }
        account.setPasswordHash(PasswordHasher.hash(request.newPassword()));
        account.setSessionVersion(account.getSessionVersion() + 1);
    }

    private SellerAccount account() {
        return accounts.findById(1L).orElseThrow(() -> new IllegalStateException("卖家账号尚未初始化"));
    }
}
