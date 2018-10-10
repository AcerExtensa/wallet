/*
 * Copyright 2014-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package xplatforms.taler.wallet.ui.send;

import android.os.Handler;
import android.os.Looper;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import xplatforms.taler.wallet.Constants;
import xplatforms.taler.wallet.R;

/**
 * @author Andreas Schildbach
 */
public final class RequestWalletBalanceTask {
    private final Handler backgroundHandler;
    private final Handler callbackHandler;
    private final ResultCallback resultCallback;

    private static final Logger log = LoggerFactory.getLogger(RequestWalletBalanceTask.class);

    public interface ResultCallback
    {
        void onResult(Coin balance);

        void onFail(int messageResId, Object... messageArgs);
    }

    public RequestWalletBalanceTask(final Handler backgroundHandler, final ResultCallback resultCallback) {
        this.backgroundHandler = backgroundHandler;
        this.callbackHandler = new Handler(Looper.myLooper());
        this.resultCallback = resultCallback;
    }

    public static class JsonRpcRequest {
        public final int id;
        public final String method;
        public final String[] params;

        private static transient int idCounter = 0;

        public JsonRpcRequest(final String method, final String[] params) {
            this.id = idCounter++;
            this.method = method;
            this.params = params;
        }
    }

    public static class JsonRpcResponse {
        public int id;
        public Utxo[] result;

        public static class Utxo {
            public String tx_hash;
            public int tx_pos;
            public long value;
            public int height;
        }
    }

    private static String getPostParamString(Hashtable<String, String> params) {
        if(params.size() == 0)
            return "";

        StringBuffer buf = new StringBuffer();
        Enumeration<String> keys = params.keys();
        while(keys.hasMoreElements()) {
            buf.append(buf.length() == 0 ? "" : "&");
            String key = keys.nextElement();
            buf.append(key).append("=").append(params.get(key));
        }
        return buf.toString();
    }


 

    public void requestWalletBalance(final String assets, final Address address)
    {
        backgroundHandler.post(new Runnable()
        {
            @Override
            public void run()
            {
                org.bitcoinj.core.Context.propagate(Constants.CONTEXT);

                String balance = requestGetBalance(address.toString());

                if(balance.isEmpty() || balance.indexOf("error") != -1)
                {
                    onFail(R.string.error_explorer_balance_wallet_not_found, "");
                    //error_explorer_balance_wallet_not_found
                }
                else
                {
                    Coin utxoValue = Coin.parseCoin(balance);
                    balance = requestGetBalance(assets);
                    onResult(utxoValue);
                }
            }
        });
    }

    protected void onResult(final Coin balance)
    {
        callbackHandler.post(new Runnable()
        {
            @Override
            public void run() {
                resultCallback.onResult(balance);
            }
        });
    }

    protected void onFail(final int messageResId, final Object... messageArgs) {
        callbackHandler.post(new Runnable()
        {
            @Override
            public void run() {
                resultCallback.onFail(messageResId, messageArgs);
            }
        });
    }

    public static class ElectrumServer {
        public enum Type {
            TCP, TLS
        }

        public final InetSocketAddress socketAddress;
        public final Type type;
        public final String certificateFingerprint;

        public ElectrumServer(final String type, final String host, final String port,
                final String certificateFingerprint) {
            this.type = Type.valueOf(type.toUpperCase());
            if (port != null)
                this.socketAddress = InetSocketAddress.createUnresolved(host, Integer.parseInt(port));
            else if ("tcp".equalsIgnoreCase(type))
                this.socketAddress = InetSocketAddress.createUnresolved(host,
                        Constants.ELECTRUM_SERVER_DEFAULT_PORT_TCP);
            else if ("tls".equalsIgnoreCase(type))
                this.socketAddress = InetSocketAddress.createUnresolved(host,
                        Constants.ELECTRUM_SERVER_DEFAULT_PORT_TLS);
            else
                throw new IllegalStateException("Cannot handle: " + type);
            this.certificateFingerprint = certificateFingerprint;
        }
    }

    private static List<ElectrumServer> loadElectrumServers(final InputStream is) throws IOException {
        final Splitter splitter = Splitter.on(':').trimResults();
        final List<ElectrumServer> servers = new LinkedList<>();
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            while (true) {
                line = reader.readLine();
                if (line == null)
                    break;
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;

                final Iterator<String> i = splitter.split(line).iterator();
                final String type = i.next();
                final String host = i.next();
                final String port = i.hasNext() ? Strings.emptyToNull(i.next()) : null;
                final String fingerprint = i.hasNext() ? Strings.emptyToNull(i.next()) : null;
                servers.add(new ElectrumServer(type, host, port, fingerprint));
            }
        } catch (final Exception x) {
            throw new RuntimeException("Error while parsing: '" + line + "'", x);
        } finally {
            if (reader != null)
                reader.close();
            is.close();
        }
        return servers;
    }

    private SSLSocketFactory sslTrustAllCertificates() {
        try {
            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, new TrustManager[] { TRUST_ALL_CERTIFICATES }, null);
            final SSLSocketFactory socketFactory = context.getSocketFactory();
            return socketFactory;
        } catch (final Exception x) {
            throw new RuntimeException(x);
        }
    }

    private static final X509TrustManager TRUST_ALL_CERTIFICATES = new X509TrustManager() {
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private String sslCertificateFingerprint(final Certificate certificate) {
        try {
            return Hashing.sha256().newHasher().putBytes(certificate.getEncoded()).hash().toString();
        } catch (final Exception x) {
            throw new RuntimeException(x);
        }
    }
}
