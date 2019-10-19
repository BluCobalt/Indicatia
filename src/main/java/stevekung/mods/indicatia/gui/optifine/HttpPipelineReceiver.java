package stevekung.mods.indicatia.gui.optifine;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;

public class HttpPipelineReceiver
extends Thread
{
    public HttpPipelineConnection httpPipelineConnection = null;
    public static final Charset ASCII = Charset.forName("ASCII");
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final char CR = '\r';
    public static final char LF = '\n';

    public HttpPipelineReceiver(HttpPipelineConnection httpPipelineConnection)
    {
        super("HttpPipelineReceiver");

        this.httpPipelineConnection = httpPipelineConnection;
    }

    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            HttpPipelineRequest currentRequest = null;
            try
            {
                currentRequest = this.httpPipelineConnection.getNextRequestReceive();

                InputStream in = this.httpPipelineConnection.getInputStream();
                HttpResponse resp = this.readResponse(in);

                this.httpPipelineConnection.onResponseReceived(currentRequest, resp);
            }
            catch (InterruptedException e)
            {
                return;
            }
            catch (Exception e)
            {
                this.httpPipelineConnection.onExceptionReceive(currentRequest, e);
            }
        }
    }

    public HttpResponse readResponse(InputStream in)
            throws IOException
    {
        String statusLine = this.readLine(in);

        String[] parts = tokenize(statusLine, " ");
        if (parts.length < 3) {
            throw new IOException("Invalid status line: " + statusLine);
        }
        int status = parseInt(parts[1], 0);
        Map<String, String> headers = new LinkedHashMap();
        for (;;)
        {
            String line = this.readLine(in);
            if (line.length() <= 0) {
                break;
            }
            int pos = line.indexOf(":");
            if (pos > 0)
            {
                String key = line.substring(0, pos).trim();
                String val = line.substring(pos + 1).trim();
                headers.put(key, val);
            }
        }
        byte[] body = null;
        String lenStr = headers.get("Content-Length");
        if (lenStr != null)
        {
            int len = parseInt(lenStr, -1);
            if (len > 0)
            {
                body = new byte[len];
                this.readFull(body, in);
            }
        }
        else
        {
            String enc = headers.get("Transfer-Encoding");
            if (equals(enc, "chunked")) {
                body = this.readContentChunked(in);
            }
        }
        return new HttpResponse(status, statusLine, headers, body);
    }

    public byte[] readContentChunked(InputStream in)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;)
        {
            String line = this.readLine(in);
            String[] parts = tokenize(line, "; ");
            int len = Integer.parseInt(parts[0], 16);
            byte[] buf = new byte[len];

            this.readFull(buf, in);
            baos.write(buf);

            this.readLine(in);
            if (len == 0) {
                break;
            }
        }
        return baos.toByteArray();
    }

    public void readFull(byte[] buf, InputStream in)
            throws IOException
    {
        int pos = 0;
        while (pos < buf.length)
        {
            int len = in.read(buf, pos, buf.length - pos);
            if (len < 0) {
                throw new EOFException();
            }
            pos += len;
        }
    }

    public String readLine(InputStream in)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int prev = -1;
        boolean hasCRLF = false;
        for (;;)
        {
            int i = in.read();
            if (i < 0) {
                break;
            }
            baos.write(i);
            if (prev == 13 && i == 10)
            {
                hasCRLF = true;
                break;
            }
            prev = i;
        }
        byte[] bytes = baos.toByteArray();

        String str = new String(bytes, ASCII);
        if (hasCRLF) {
            str = str.substring(0, str.length() - 2);
        }
        return str;
    }


    public static String[] tokenize(String str, String delim)
    {
        StringTokenizer tok = new StringTokenizer(str, delim);
        List<String> list = new ArrayList<>();
        while (tok.hasMoreTokens())
        {
            String token = tok.nextToken();
            list.add(token);
        }
        String[] strs = list.toArray(new String[list.size()]);
        return strs;
    }

    public static int parseInt(String str, int defVal)
    {
        try
        {
            if (str == null) {
                return defVal;
            }
            str = str.trim();

            return Integer.parseInt(str);
        }
        catch (NumberFormatException e) {}
        return defVal;
    }

    public static boolean equals(Object o1, Object o2)
    {
        if (o1 == o2) {
            return true;
        }
        if (o1 == null) {
            return false;
        }
        return o1.equals(o2);
    }
}
