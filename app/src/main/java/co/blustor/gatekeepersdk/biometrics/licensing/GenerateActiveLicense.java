package co.blustor.gatekeepersdk.biometrics.licensing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import co.blustor.gatekeepersdk.biometrics.BiometricLicenseManager;
import co.blustor.gatekeepersdk.data.GKFile;
import co.blustor.gatekeepersdk.services.GKFileActions;
import co.blustor.gatekeepersdk.utils.GKFileUtils;

/**
 * Intended for internal use only.
 */
public class GenerateActiveLicense {
    private final BiometricLicenseManager mLicenseManager;
    protected GKFileActions mFileActions;
    protected String mLicenseSubDir;

    public GenerateActiveLicense(BiometricLicenseManager licenseManager, GKFileActions fileActions, String licenseSubDir) {
        mLicenseManager = licenseManager;
        mFileActions = fileActions;
        mLicenseSubDir = licenseSubDir;
    }

    public String execute(GKFile serialNumberFile) throws IOException {
        GKFileActions.GetFileResult fileResult = getSerialNumberFile(serialNumberFile);
        if (fileResult.getStatus() != GKFileActions.Status.SUCCESS) {
            throw new IOException("Could not retrieve serial number file");
        }

        String serialNumber = GKFileUtils.readFile(fileResult.getFile());

        GKFileActions.FileResult createLicenseSubdirectoryResult = createLicenseSubdirectory();
        if (createLicenseSubdirectoryResult.getStatus() != GKFileActions.Status.SUCCESS) {
            throw new IOException("Could not create license subdirectory: " + mLicenseSubDir);
        }

        GKFileActions.PutFileResult putSerialNumberFileResult = storeSerialNumberFile(serialNumberFile, serialNumber);
        if (putSerialNumberFileResult.getStatus() != GKFileActions.Status.SUCCESS) {
            throw new IOException("Could not create serial number file");
        }

        String generatedId = mLicenseManager.generateID(serialNumber);
        GKFileActions.PutFileResult putIdFileResult = storeIdFile(serialNumberFile, generatedId);
        if (putIdFileResult.getStatus() != GKFileActions.Status.SUCCESS) {
            throw new IOException("Could not create ID file");
        }

        String license = null;
        GKFileActions.PutFileResult putLicenseFileResult = null;
        try {
            license = mLicenseManager.activateOnline(generatedId);
            putLicenseFileResult = storeLicenseFile(serialNumberFile, license);
            if (putLicenseFileResult.getStatus() != GKFileActions.Status.SUCCESS) {
                throw new IOException("Could not create license file");
            }

            GKFileActions.FileResult deleteSerialFileResult = mFileActions.deleteFile(serialNumberFile);
            if (deleteSerialFileResult.getStatus() != GKFileActions.Status.SUCCESS) {
                throw new IOException("Could not delete license file from root");
            }

            return license;
        } catch (IOException e) {
            cleanupLicenses(license, putLicenseFileResult);
            throw (e);
        }
    }

    private GKFileActions.FileResult createLicenseSubdirectory() throws IOException {
        GKFileActions.ListFilesResult licenseDirList = mFileActions.listFiles(GKFileUtils.LICENSE_ROOT);
        for (GKFile file : licenseDirList.getFiles()) {
            if (file.isDirectory() && mLicenseSubDir.equals(file.getName())) {
                return new GKFileActions.FileResult(GKFileActions.Status.SUCCESS);
            }
        }
        return mFileActions.makeDirectory(buildPathFromLicenseSubDir());
    }

    private void cleanupLicenses(String license, GKFileActions.PutFileResult putLicenseFileResult) throws IOException {
        if (license != null) {
            mLicenseManager.deactivateOnline(license);
        }
        if (putLicenseFileResult != null && putLicenseFileResult.getFile() != null) {
            mFileActions.deleteFile(putLicenseFileResult.getFile());
        }
    }

    private GKFileActions.GetFileResult getSerialNumberFile(GKFile serialNumberFile) throws IOException {
        File tempFile = File.createTempFile(serialNumberFile.getName(), LicenseFileExtensions.SERIAL_NUMBER);
        return mFileActions.getFile(serialNumberFile, tempFile);
    }

    private GKFileActions.PutFileResult storeSerialNumberFile(GKFile serialNumberFile, String serialNumber) throws IOException {
        String serialNumberCardPath = buildPathFromLicenseSubDir(serialNumberFile.getName());
        ByteArrayInputStream serialNumberStream = new ByteArrayInputStream(serialNumber.getBytes());
        return mFileActions.putFile(serialNumberStream, serialNumberCardPath);
    }

    private GKFileActions.PutFileResult storeIdFile(GKFile serialNumberFile, String generatedId) throws IOException {
        String idCardPath = buildPathFromLicenseSubDir(GKFileUtils.addExtension(serialNumberFile.getFilenameBase(), LicenseFileExtensions.ID));
        return mFileActions.putFile(new ByteArrayInputStream(generatedId.getBytes()), idCardPath);
    }

    private GKFileActions.PutFileResult storeLicenseFile(GKFile serialNumberFile, String license) throws IOException {
        String licenseCardPath = buildPathFromLicenseSubDir(GKFileUtils.addExtension(serialNumberFile.getFilenameBase(), LicenseFileExtensions.LICENSE));
        return mFileActions.putFile(new ByteArrayInputStream(license.getBytes()), licenseCardPath);
    }

    private String buildPathFromLicenseSubDir(String... parts) {
        String path = GKFileUtils.joinPath(parts);
        return GKFileUtils.joinPath(GKFileUtils.LICENSE_ROOT, mLicenseSubDir, path);
    }
}
