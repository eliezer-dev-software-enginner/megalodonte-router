package megalodonte.router.v4;

public class RouteResolutionException extends RuntimeException {
    public RouteResolutionException(String path, Throwable cause) {
        super("Falha ao resolver rota: " + path, cause);
    }
}