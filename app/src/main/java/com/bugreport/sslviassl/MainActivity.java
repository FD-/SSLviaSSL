package com.bugreport.sslviassl;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.PrintWriter;
import java.io.StringWriter;

import static android.view.inputmethod.EditorInfo.IME_ACTION_GO;

public class MainActivity extends AppCompatActivity {
    private static final String DESCRIPTION = "\n\n\n\n" +
            "This example tries to connect to an HTTPS server via a Secure Web Proxy.\n" +
            "A Secure Web Proxy is an HTTP proxy secured via SSL/TLS.\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText proxyHostEditText = (EditText) findViewById(R.id.proxyHostEditText);
        final EditText proxyPortEditText = (EditText) findViewById(R.id.proxyPortEditText);
        final EditText destinationUrlEditText = (EditText) findViewById(R.id.destinationUrlEditText);
        final TextView resultText = (TextView) findViewById(R.id.resultText);
        final Button sendButton = (Button) findViewById(R.id.sendButton);

        proxyHostEditText.setText(Main.PROXY_HOST);
        proxyPortEditText.setText(Integer.toString(Main.PROXY_PORT));
        destinationUrlEditText.setText(Main.DESTINATION_URL);
        resultText.setText(DESCRIPTION);

        destinationUrlEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == IME_ACTION_GO){
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    sendButton.performClick();
                    return true;
                }
                return false;
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(destinationUrlEditText.getWindowToken(), 0);

                String proxyHost = proxyHostEditText.getText().toString();
                int proxyPort = Integer.parseInt(proxyPortEditText.getText().toString());
                String destinationUrl = destinationUrlEditText.getText().toString();

                SecureWebProxyThread thread = new SecureWebProxyThread(proxyHost, proxyPort, destinationUrl);
                SecureWebProxyThread.ProxyThreadResultListener listener = new SecureWebProxyThread.ProxyThreadResultListener() {
                    @Override
                    public void onResultReceived(final String result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultText.setText(result);
                            }
                        });
                    }

                    @Override
                    public void onException(final Exception exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                StringWriter errorString = new StringWriter();
                                exception.printStackTrace(new PrintWriter(errorString));
                                resultText.setText("Exception: \n" + errorString);
                            }
                        });
                    }
                };
                thread.setListener(listener);
                thread.start();
            }
        });
    }
}
