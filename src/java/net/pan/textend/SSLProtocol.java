package net.pan.textend;

/**
 * Versions of SSL/TLS.
 * Constant order is guaranteed to be from most recent to least.
 */
public enum SSLProtocol
{
    TLSv1_2("TLSv1.2"),
    TLSv1_1("TLSv1.1"),
    TLSv1,
    SSLv3,
    SSLv2,
    SSL;

    /**
     * Standard Java algorithm name.
     *
     * @see <a href="https://docs.oracle.com/javase/9/docs/specs/security/standard-names.html#sslcontext-algorithms">SSLContext algorithms</a>
     */
    final String standardName;

    private SSLProtocol()
    {
        this.standardName = name();
    }

    private SSLProtocol(String standardName)
    {
        this.standardName = standardName;
    }

    static SSLProtocol defaultProtocol()
    {
        // Assume first constant is most recent.
        return values()[0];
    }
}
