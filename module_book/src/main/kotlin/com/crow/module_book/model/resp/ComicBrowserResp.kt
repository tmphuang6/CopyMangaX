package com.crow.module_book.model.resp

import com.crow.module_book.model.resp.comic_browser.Browse
import com.squareup.moshi.Json

data class ComicBrowserResp(

    @Json(name =  "browse")
    val mBrowse: Browse?,

    @Json(name =  "collect")
    val mCollectId: Int?,

    @Json(name =  "is_lock")
    val mIsLock: Boolean,

    @Json(name =  "is_login")
    val mIsLogin: Boolean,

    @Json(name =  "is_mobile_bind")
    val mIsMobileBind: Boolean,

    @Json(name =  "is_vip")
    val mIsVip: Boolean,
)