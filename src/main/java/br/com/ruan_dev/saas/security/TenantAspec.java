package br.com.ruan_dev.saas.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantAspec {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* br.com.ruan_dev.saas.repositories.*.*(..))")
    public void beforeRepositoryExecution(JoinPoint joinPoint){
        String tenantIdStr = TenantContext.getCurrentTenant();

        if (tenantIdStr != null){
            Long tenantId = Long.valueOf(tenantIdStr);
            Session session = entityManager.unwrap(Session.class);
            org.hibernate.Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("tenantId", tenantId);
        }
    }
}
