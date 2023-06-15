package com.crow.module_book.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import com.crow.base.app.appContext
import com.crow.base.tools.extensions.BASE_ANIM_200L
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.tools.extensions.isDarkMode
import com.crow.base.ui.view.ToolTipsView
import com.crow.module_book.R
import com.crow.module_book.databinding.BookFragmentChapterRvBinding
import com.crow.module_book.model.resp.comic_chapter.ComicChapterResult
import kotlinx.coroutines.delay

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: module_comic/src/main/kotlin/com/crow/module_comic/ui/adapter
 * @Time: 2023/3/15 16:42
 * @Author: CrowForKotlin
 * @Description: ComicInfoChapterRvAdapter
 * @formatter:on
 **************************/

class ComicChapterRvAdapter(
    private var mComic: MutableList<ComicChapterResult> = mutableListOf(),
    private var mDoOnTapChapter: (ComicChapterResult) -> Unit
) : RecyclerView.Adapter<ComicChapterRvAdapter.ViewHolder>() {

    var mChapterName: String? = null

    private var mBtSurfaceColor: Int = if (isDarkMode()) ContextCompat.getColor(appContext, com.google.android.material.R.color.m3_sys_color_dark_surface) else ContextCompat.getColor(appContext, R.color.book_button_bg_white)
    private var mBtTextColor = if (isDarkMode()) ContextCompat.getColor(appContext, R.color.book_button_bg_white) else ContextCompat.getColor(appContext, R.color.book_button_text_purple)

    inner class ViewHolder(rvBinding: BookFragmentChapterRvBinding) : RecyclerView.ViewHolder(rvBinding.root) { val mButton = rvBinding.comicInfoRvChip }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(BookFragmentChapterRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)).also { vh ->
            vh.mButton.doOnClickInterval { mDoOnTapChapter(mComic[vh.absoluteAdapterPosition]) }
            vh.mButton.doOnLayout {
                ToolTipsView.showToolTipsByLongClick(vh.mButton, it.measuredWidth shr 2)
            }
        }
    }

    override fun getItemCount(): Int = mComic.size

    override fun onBindViewHolder(vh: ViewHolder, position: Int) {

        val comic = mComic[position]
        vh.mButton.text = comic.name
        if (mChapterName != null && comic.name == mChapterName!!) {
            vh.mButton.setBackgroundColor(ContextCompat.getColor(vh.itemView.context, R.color.book_blue))
            vh.mButton.setTextColor(ContextCompat.getColor(vh.itemView.context, android.R.color.white))
        } else {
            vh.mButton.background.setTint(mBtSurfaceColor)
            vh.mButton.setTextColor(mBtTextColor)
        }
    }

    suspend fun doNotify(newDataResult: MutableList<ComicChapterResult>, delayMs: Long = 1L) {
        val isCountSame = itemCount == newDataResult.size
        if (isCountSame) mComic = newDataResult
        else if(itemCount != 0) {
            notifyItemRangeRemoved(0, itemCount)
            mComic.clear()
            delay(BASE_ANIM_200L)
        }
        newDataResult.forEachIndexed { index, data ->
            if (!isCountSame) {
                mComic.add(data)
                notifyItemInserted(index)
            } else notifyItemChanged(index)
            delay(delayMs)
        }
    }
}