package br.com.ruan_dev.saas.security;

public class TenantContext {

    private static ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static ThreadLocal<String> CURRENT_USUARIO_EMAIL = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId){
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant(){
        return CURRENT_TENANT.get();
    }

    public static void setCurrentUsuarioEmail(String usuarioEmail){
        CURRENT_USUARIO_EMAIL.set(usuarioEmail);
    }

    public static String getCurrentUsuarioEmail(){
        return CURRENT_USUARIO_EMAIL.get();
    }

    public static void clear(){
        CURRENT_TENANT.remove();
        CURRENT_USUARIO_EMAIL.remove();
    }


}
