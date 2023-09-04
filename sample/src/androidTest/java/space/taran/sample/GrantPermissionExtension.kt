package dev.arkbuilders.sample

import android.Manifest
import android.os.Build
import androidx.test.internal.platform.ServiceLoaderWrapper.loadSingleService
import androidx.test.internal.platform.content.PermissionGranter
import androidx.test.runner.permission.PermissionRequester
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * https://github.com/mannodermaus/android-junit5/issues/251
 *
 * The {@code GrantPermissionExtension} allows granting of runtime permissions on Android M (API 23)
 * and above. Use this extension when a test requires a runtime permission to do its work.
 *
 * <p>When applied to a test class it attempts to grant all requested runtime permissions.
 * The requested permissions will then be granted on the device and will take immediate effect.
 * Permissions can only be requested on Android M (API 23) or above and will be ignored on all other
 * API levels. Once a permission is granted it will apply for all tests running in the current
 * Instrumentation. There is no way of revoking a permission after it was granted. Attempting to do
 * so will crash the Instrumentation process.
 */
class GrantPermissionExtension private constructor(
    permissions: Set<String>,
    private val granter: PermissionGranter = loadSingleService(
        PermissionGranter::class.java,
        ::PermissionRequester
    )
) : BeforeEachCallback {

    companion object {
        @JvmStatic
        fun grant(vararg permissions: String): GrantPermissionExtension {
            val permissionSet = satisfyPermissionDependencies(*permissions)
            return GrantPermissionExtension(permissionSet)
        }

        private fun satisfyPermissionDependencies(vararg permissions: String): Set<String> {
            val set = permissions.toMutableSet()

            // Grant READ_EXTERNAL_STORAGE implicitly if its counterpart is present
            if (Build.VERSION.SDK_INT >= 16 && Manifest.permission.WRITE_EXTERNAL_STORAGE in set) {
                set.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            return set
        }
    }

    init {
        granter.addPermissions(*permissions.toTypedArray())
    }

    override fun beforeEach(context: ExtensionContext?) {
        granter.requestPermissions()
    }
}