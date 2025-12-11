package codeit.sb06.imagepost.security;

import codeit.sb06.imagepost.security.evaluator.DomainPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalPermissionEvaluator implements PermissionEvaluator {

    private final Map<String, DomainPermissionEvaluator> evaluatorMap;

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        log.info("Checking permission for targetType: {}", targetType);

        DomainPermissionEvaluator evaluator = evaluatorMap.get(targetType);

        if (evaluator == null) {
            throw new IllegalArgumentException("지원하지 않는 TargetType입니다: " + targetType);
        }
        return evaluator.hasPermission(authentication, targetId, (String) permission);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false;
    }
}