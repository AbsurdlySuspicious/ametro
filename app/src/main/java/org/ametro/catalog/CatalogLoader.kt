package org.ametro.catalog

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import org.ametro.app.ApplicationEx
import org.ametro.catalog.entities.MapCatalog

class MapCatalogAsyncTaskLoaderLocal(private val app: ApplicationEx?, context: Context) :
    AsyncTaskLoader<MapCatalog?>(context) {
    override fun loadInBackground(): MapCatalog {
        return app!!.getLocalMapCatalogManager().mapCatalog
    }
}

class MapCatalogAsyncTaskLoaderRemote(private val app: ApplicationEx?, context: Context) :
    AsyncTaskLoader<MapCatalog?>(context) {
    override fun loadInBackground(): MapCatalog? {
        return app!!.getRemoteMapCatalogProvider().getMapCatalog(false)
    }
}

interface CatalogLoaderCallbacks {
    fun onCreateLoader(id: Int, args: Bundle?, loader: Loader<MapCatalog?>)
    fun onLoadFinished(loader: Loader<MapCatalog?>, data: MapCatalog?)
    fun onLoaderReset(loader: Loader<MapCatalog?>)
}

class CatalogLoader(
    private val context: Context,
    private val listener: CatalogLoaderCallbacks?
) :
    LoaderManager.LoaderCallbacks<MapCatalog?> {

    companion object {
        const val LOCAL_CATALOG_LOADER = 1
        const val REMOTE_CATALOG_LOADER = 2
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<MapCatalog?> {
        val app = ApplicationEx.getInstanceContext(context.applicationContext)
        val loader: AsyncTaskLoader<MapCatalog?> = when (id) {
            LOCAL_CATALOG_LOADER -> MapCatalogAsyncTaskLoaderLocal(app, context)
            REMOTE_CATALOG_LOADER -> MapCatalogAsyncTaskLoaderRemote(app, context)
            else -> throw Exception("Unknown loader $id")
        }
        listener?.onCreateLoader(id, args, loader)
        return loader
    }

    override fun onLoadFinished(loader: Loader<MapCatalog?>, data: MapCatalog?) {
        listener?.onLoadFinished(loader, data)
    }

    override fun onLoaderReset(loader: Loader<MapCatalog?>) {
        listener?.onLoaderReset(loader)
    }

}
