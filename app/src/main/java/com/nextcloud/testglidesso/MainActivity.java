package com.nextcloud.testglidesso;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.GsonBuilder;
import com.nextcloud.android.sso.AccountImporter;
import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.nextcloud.android.sso.api.Response;
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException;
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted;
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException;
import com.nextcloud.android.sso.helper.SingleAccountHelper;
import com.nextcloud.android.sso.model.SingleSignOnAccount;
import com.nextcloud.android.sso.ui.UiExceptionManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button chooseAccount = findViewById(R.id.choose_button);
        chooseAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAccountChooser();
            }
        });
    }

    private void openAccountChooser() {
        try {
            AccountImporter.pickNewAccount(this);
        } catch (NextcloudFilesAppNotInstalledException | AndroidGetAccountsPermissionNotGranted e) {
            UiExceptionManager.showDialogForException(this, e);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            AccountImporter.onActivityResult(requestCode, resultCode, data, this, new AccountImporter.IAccountAccessGranted() {
                @Override
                public void accountAccessGranted(SingleSignOnAccount account) {
                    Context l_context = getApplicationContext();
                    SingleAccountHelper.setCurrentAccount(l_context, account.name);

                    accountAccessDone(account);
                }
            });
        } catch (AccountImportCancelledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AccountImporter.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void accountAccessDone(SingleSignOnAccount account) {
        NextcloudAPI client = new NextcloudAPI(this, account, new GsonBuilder().create(), new NextcloudAPI.ApiConnectedListener() {
            @Override
            public void onConnected() {
                Log.v(TAG, "SSO API successfully initialized");
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }
        });

        new Thread(() -> {
            NextcloudRequest.Builder requestBuilder;
            try {
                requestBuilder = new NextcloudRequest.Builder()
                        .setMethod("GET")
                        .setUrl("/index.php/core/preview?fileId=71520&c=0bf5d05b8ddb80890d3835eb17f006e7&x=250&y=250&forceIcon=0");
                NextcloudRequest nextcloudRequest = requestBuilder.build();
                Log.v(TAG, nextcloudRequest.toString());
                Response response = client.performNetworkRequestV2(nextcloudRequest);
                response.getBody();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}