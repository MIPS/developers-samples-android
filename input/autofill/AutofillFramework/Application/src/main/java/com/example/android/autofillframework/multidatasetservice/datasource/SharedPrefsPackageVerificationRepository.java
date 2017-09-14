/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofillframework.multidatasetservice.datasource;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static com.example.android.autofillframework.CommonUtil.TAG;

public class SharedPrefsPackageVerificationRepository implements PackageVerificationDataSource {

    private static final String SHARED_PREF_KEY = "com.example.android.autofillframework"
            + ".multidatasetservice.datasource.PackageVerificationDataSource";
    private static PackageVerificationDataSource sInstance;

    private SharedPrefsPackageVerificationRepository() {
    }

    public static PackageVerificationDataSource getInstance() {
        if (sInstance == null) {
            sInstance = new SharedPrefsPackageVerificationRepository();
        }
        return sInstance;
    }

    @Override
    public void clear(Context context) {
        context.getApplicationContext().getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    @Override
    public boolean putPackageSignatures(Context context, String packageName) {
        String hash;
        try {
            hash = getCertificateHash(context, packageName);
            Log.d(TAG, "Hash for " + packageName + ": " + hash);
        } catch (Exception e) {
            Log.w(TAG, "Error getting hash for " + packageName + ": " + e);
            return false;
        }

        if (!containsSignatureForPackage(context, packageName)) {
            // Storage does not yet contain signature for this package name.
            context.getApplicationContext()
                    .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
                    .edit()
                    .putString(packageName, hash)
                    .apply();
            return true;
        }
        return containsMatchingSignatureForPackage(context, packageName, hash);
    }

    private boolean containsSignatureForPackage(Context context, String packageName) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(
                SHARED_PREF_KEY, Context.MODE_PRIVATE);
        return prefs.contains(packageName);
    }

    private boolean containsMatchingSignatureForPackage(Context context, String packageName,
            String hash) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(
                SHARED_PREF_KEY, Context.MODE_PRIVATE);
        return hash.equals(prefs.getString(packageName, null));
    }

    private String getCertificateHash(Context context, String packageName)
            throws Exception {
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        Signature[] signatures = packageInfo.signatures;
        byte[] cert = signatures[0].toByteArray();
        try (InputStream input = new ByteArrayInputStream(cert)) {
            CertificateFactory factory = CertificateFactory.getInstance("X509");
            X509Certificate x509 = (X509Certificate) factory.generateCertificate(input);
            MessageDigest md = MessageDigest.getInstance("SHA256");
            byte[] publicKey = md.digest(x509.getEncoded());
            return toHexFormat(publicKey);
        }
    }

    private String toHexFormat(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i]);
            int length = hex.length();
            if (length == 1) {
                hex = "0" + hex;
            }
            if (length > 2) {
                hex = hex.substring(length - 2, length);
            }
            builder.append(hex.toUpperCase());
            if (i < (bytes.length - 1)) {
                builder.append(':');
            }
        }
        return builder.toString();
    }
}
