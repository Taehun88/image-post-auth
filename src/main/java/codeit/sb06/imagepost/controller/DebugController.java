package codeit.sb06.imagepost.controller;


import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DebugController {

    private final SessionRegistry sessionRegistry;

    @GetMapping("/api/debug/context")
    public String getContextInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null || !authentication.isAuthenticated()){
            return "Current Context: Anonymous User";
        }

        return "Current Context: " + authentication.getName();
    }

    @GetMapping("/api/debug/registry")
    public List<String> getRegistryInfo() {
        return sessionRegistry.getAllPrincipals().stream().map(Object::toString).toList();
    }
}
