package com.mg.structuralai;

import android.os.Build;
import java.util.Locale;

/**
 * Runtime product-release acceptance gate. This does not replace engineering validation;
 * it separates a numerically qualified test candidate from an actual P1 production release.
 */
public final class P1ReleaseAcceptanceGate {
    public static final class Result {
        public final boolean testCandidateReady;
        public final boolean productReady;
        public final boolean packageIdentityOk, versionCodeOk, arm64Ok, occtOk, goldenOk, reportEvidenceOk, productionBuild, productionSigningVerified;
        public final String summary;
        Result(boolean testReady, boolean productReady,
               boolean packageIdentityOk, boolean versionCodeOk, boolean arm64Ok, boolean occtOk,
               boolean goldenOk, boolean reportEvidenceOk, boolean productionBuild, boolean productionSigningVerified, String summary){
            this.testCandidateReady=testReady;this.productReady=productReady;
            this.packageIdentityOk=packageIdentityOk;this.versionCodeOk=versionCodeOk;this.arm64Ok=arm64Ok;this.occtOk=occtOk;
            this.goldenOk=goldenOk;this.reportEvidenceOk=reportEvidenceOk;this.productionBuild=productionBuild;
            this.productionSigningVerified=productionSigningVerified;this.summary=summary;
        }
    }
    private P1ReleaseAcceptanceGate(){}

    public static Result run(boolean goldenOk, boolean reportEvidenceOk){
        boolean packageIdentityOk="com.mg.structuralai".equals(BuildConfig.APPLICATION_ID);
        // CI stamps 10000 + GITHUB_RUN_NUMBER. Source-tree baselines remain lower by design.
        boolean versionCodeOk=BuildConfig.VERSION_CODE>=10000;
        boolean arm64Ok=false;
        if(Build.SUPPORTED_ABIS!=null)for(String abi:Build.SUPPORTED_ABIS)if("arm64-v8a".equalsIgnoreCase(abi)){arm64Ok=true;break;}
        boolean occtOk=NativeOcctBridge.isAvailable();
        boolean productionBuild="release".equalsIgnoreCase(BuildConfig.BUILD_TYPE);

        // Fail closed: a release build is not sufficient evidence of production identity.
        // This flag remains false until the dedicated production signing certificate is provisioned
        // and its SHA-256 fingerprint is verified by both CI and installed-package runtime checks.
        boolean productionSigningVerified=false;

        boolean testReady=packageIdentityOk&&versionCodeOk&&arm64Ok&&occtOk&&goldenOk&&reportEvidenceOk;
        boolean productReady=testReady&&productionBuild&&productionSigningVerified;
        String state=productReady?"P1_PRODUCT_READY":(testReady?"P1_TEST_CANDIDATE_READY / PRODUCTION_SIGNING_REQUIRED":"P1_BLOCKED");
        String summary=String.format(Locale.US,
            "P1 RELEASE ACCEPTANCE %s | package=%s | versionCode=%d versionName=%s | arm64=%s | OCCT=%s | golden=%s | reportEvidence=%s | buildType=%s | productionBuild=%s | productionSigningVerified=%s",
            state,BuildConfig.APPLICATION_ID,BuildConfig.VERSION_CODE,BuildConfig.VERSION_NAME,arm64Ok,occtOk,goldenOk,reportEvidenceOk,BuildConfig.BUILD_TYPE,productionBuild,productionSigningVerified);
        return new Result(testReady,productReady,packageIdentityOk,versionCodeOk,arm64Ok,occtOk,goldenOk,reportEvidenceOk,productionBuild,productionSigningVerified,summary);
    }
}
