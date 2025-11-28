
package k.nutriguard.repository

import k.nutriguard.db.DBInterface
import k.nutriguard.db.DBRepository

object DBModule {
    // Single shared instance for the whole app
    val db: DBInterface = DBRepository(SupabaseProvider.client)
}
