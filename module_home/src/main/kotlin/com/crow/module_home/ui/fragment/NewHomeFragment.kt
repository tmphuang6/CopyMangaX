@file:SuppressWarnings("RestrictedApi")
package com.crow.module_home.ui.fragment

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import androidx.recyclerview.widget.GridLayoutManager
import com.crow.base.copymanga.BaseEventEnum
import com.crow.base.copymanga.BaseStrings
import com.crow.base.copymanga.entity.Fragments
import com.crow.base.tools.coroutine.FlowBus
import com.crow.base.tools.coroutine.baseCoroutineException
import com.crow.base.tools.coroutine.launchDelay
import com.crow.base.tools.extensions.BASE_ANIM_200L
import com.crow.base.tools.extensions.BASE_ANIM_300L
import com.crow.base.tools.extensions.animateFadeIn
import com.crow.base.tools.extensions.animateFadeOut
import com.crow.base.tools.extensions.animateFadeOutWithEndInVisibility
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.tools.extensions.immersionPadding
import com.crow.base.tools.extensions.isDarkMode
import com.crow.base.tools.extensions.navigateIconClickGap
import com.crow.base.tools.extensions.navigateToWithBackStack
import com.crow.base.tools.extensions.toast
import com.crow.base.tools.extensions.withLifecycle
import com.crow.base.ui.fragment.BaseMviFragment
import com.crow.base.ui.view.event.BaseEvent
import com.crow.base.ui.viewmodel.doOnError
import com.crow.base.ui.viewmodel.doOnResult
import com.crow.base.ui.viewmodel.doOnSuccess
import com.crow.module_home.R
import com.crow.module_home.databinding.HomeFragmentNewBinding
import com.crow.module_home.databinding.HomeFragmentSearchViewBinding
import com.crow.module_home.model.intent.HomeIntent
import com.crow.module_home.ui.adapter.NewHomeBannerRvAdapter
import com.crow.module_home.ui.adapter.NewHomeComicRvAdapter
import com.crow.module_home.ui.adapter.NewHomeVpAdapter
import com.crow.module_home.ui.compose.Banner
import com.crow.module_home.ui.viewmodel.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.core.qualifier.named
import com.crow.base.R as baseR

class NewHomeFragment : BaseMviFragment<HomeFragmentNewBinding>() {

    /**
     * ● 静态区
     *
     * ● 2023-09-17 01:28:03 周日 上午
     */
    companion object {
        const val HOME = "Home"
        const val SEARCH_TAG = "INPUT"
    }

    /**
     * ● 主页 VM
     *
     * ● 2023-09-17 01:27:48 周日 上午
     */
    private val mHomeVM by sharedViewModel<HomeViewModel>()


    /**
     * ● 推荐 “换一批” 刷新按钮
     *
     * ● 2023-09-17 01:27:01 周日 上午
     */
    private var mRecRefresh: MaterialButton? = null

    /**
     * ● 主页数据量较多， 采用Rv方式
     *
     * ● 2023-09-17 01:26:14 周日 上午
     */
    private val mDataRvAdapter by lazy {
        NewHomeComicRvAdapter(
            mOnRefresh = { button ->
                button.isEnabled = false
                mRecRefresh = button
                mHomeVM.input(HomeIntent.GetRecPageByRefresh())
            },
            mOnClick = { pathword -> navigateBookComicInfo(pathword) },
            mOnTopic = { }
        )
    }

    /**
     * ● Banner 轮播图
     *
     * ● 2023-09-17 01:26:42 周日 上午
     */
    private val mBannerRvAdapter by lazy { NewHomeBannerRvAdapter { pathword -> navigateBookComicInfo(pathword) } }

    /**
     * ● 全局 Event 事件
     *
     * ● 2023-09-17 01:28:34 周日 上午
     */
    private val mBaseEvent = BaseEvent.getSIngleInstance()

    /**
     * ● 漫画搜索碎片
     *
     * ● 2023-09-17 01:47:09 周日 上午
     */
    private var mSearchBinding: HomeFragmentSearchViewBinding? = null

    /**
     * ● 注册FlowBus 设置主页头像
     *
     * ● 2023-09-17 01:28:24 周日 上午
     */
    init {
        FlowBus.with<Drawable>(BaseEventEnum.SetIcon.name).register(this) { drawable ->
            if (!isHidden) {
                lifecycleScope.launch(CoroutineName(this::class.java.simpleName) + baseCoroutineException) {
                    withStarted {
                        mBinding.homeToolbar.navigationIcon = drawable
                    }
                }
            }
        }
    }

    /**
     * ● 导航至BookComicInfo
     *
     * ● 2023-06-16 22:18:11 周五 下午
     */
    private fun navigateBookComicInfo(pathword: String) {
        val tag = Fragments.BookComicInfo.name
        val bundle = Bundle()
        bundle.putString(BaseStrings.PATH_WORD, pathword)
        requireParentFragment().parentFragmentManager.navigateToWithBackStack(baseR.id.app_main_fcv,
            requireActivity().supportFragmentManager.findFragmentByTag(Fragments.Container.name)!!,
            get<Fragment>(named(tag)).also { it.arguments = bundle }, tag, tag)
    }

    /**
     * ● 导航至BookNovelInfo
     *
     * ● 2023-06-16 22:17:57 周五 下午
     */
    private fun navigateBookNovelInfo(pathword: String) {
        val tag = Fragments.BookNovelInfo.name
        val bundle = Bundle()
        bundle.putSerializable(BaseStrings.PATH_WORD, pathword)
        requireParentFragment().parentFragmentManager.navigateToWithBackStack(
            com.crow.base.R.id.app_main_fcv,
            requireActivity().supportFragmentManager.findFragmentByTag(Fragments.Container.name)!!,
            get<Fragment>(named(tag)).also { it.arguments = bundle }, tag, tag
        )
    }

    /** ● 加载主页数据 */
    private fun doLoadHomePage() {

        if (mBaseEvent.getBoolean("HOME_FRAGMENT_LOAD_HOME_PAGE") == true) return
        mBaseEvent.setBoolean("HOME_FRAGMENT_LOAD_HOME_PAGE", true)

        // 错误提示 可见
        if (mBinding.homeTipsError.isVisible) {
            mBinding.homeTipsError.isVisible = false
            mBinding.homeRv.animateFadeIn()
        }

        // Banner 不可见 谈出
        if (mBinding.homeComposeBanner.isGone) mBinding.homeComposeBanner.animateFadeIn()

        onPageRvChaned()
    }

    private fun onPageRvChaned() {
        viewLifecycleOwner.lifecycleScope.launch {

            async {
                mBinding.homeComposeBanner.setContent {
                    Banner(banners = mHomeVM.getSnapshotBanner().toList()) { }
                }
                yield()
            }.await()

            finishRefresh()

            mDataRvAdapter.doNotify(mHomeVM.getSnapshotHomeData(), 50L)
        }
    }

    suspend fun finishRefresh() {
        mBinding.homeRefresh.finishRefresh()
        delay(BASE_ANIM_200L shl 2)
    }

    /** ● 导航至设置Fragment */
    private fun navigateSettings() {
        requireParentFragment().parentFragmentManager.navigateToWithBackStack(baseR.id.app_main_fcv,
            requireActivity().supportFragmentManager.findFragmentByTag(Fragments.Container.name)!!,
            get(named(Fragments.Settings.name)), Fragments.Settings.name, Fragments.Settings.name
        )
    }

    /** ● 初始化SearchView */
    @SuppressLint("PrivateResource")
    private fun initSearchView() {
        if (mBaseEvent.getBoolean("HOME_FRAGMENT_INIT_SEARCH_VIEW") == true) return
        mBaseEvent.setBoolean("HOME_FRAGMENT_INIT_SEARCH_VIEW", true)
        mBinding.homeSearchView.apply {
            val binding = HomeFragmentSearchViewBinding.inflate(layoutInflater).also { mSearchBinding = it }                                                                 // 获取SearchViewBinding
            val searchComicFragment = SearchComicFragment.newInstance(mBinding.homeSearchView) { navigateBookComicInfo(it) }   // 实例化SearchComicFragment
            val searchNovelFragment = SearchNovelFragment.newInstance(mBinding.homeSearchView) { navigateBookNovelInfo(it) }     // 实例化SearchNovelFragment

            val bgColor: Int; val tintColor: Int;
//            val statusBarDrawable: Drawable?
            if (isDarkMode()) {
                bgColor = ContextCompat.getColor(mContext, com.google.android.material.R.color.m3_sys_color_dark_surface)
                tintColor = ContextCompat.getColor(mContext, android.R.color.white)
//                statusBarDrawable = AppCompatResources.getDrawable(mContext, com.google.android.material.R.color.m3_sys_color_dark_surface)
            } else {
                bgColor = ContextCompat.getColor(mContext, android.R.color.white)
                tintColor = ContextCompat.getColor(mContext, android.R.color.black)
//                statusBarDrawable = AppCompatResources.getDrawable(mContext, baseR.color.base_white)
            }
            toolbar.setNavigationIcon(baseR.drawable.base_ic_back_24dp)                                                                             // 设置SearchView toolbar导航图标
            toolbar.navigationIcon?.setTint(tintColor)
            toolbar.setBackgroundColor(bgColor)                                                                                                                    // 设置SearchView toolbar背景色白，沉浸式
            binding.homeSearchVp.setBackgroundColor(bgColor)
            setStatusBarSpacerEnabled(false)                                                                                                                          // 关闭状态栏空格间距

            // 添加一个自定义 View设置其高度为StatubarHeight实现沉浸式效果
            /*addHeaderView(View(mContext).also { view->
                view.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mContext.getStatusBarHeight())
                view.foreground = statusBarDrawable
            })*/

            addView(binding.root)                                                                                                         // 添加SearcViewBinding 视图内容
            binding.homeSearchVp.adapter = NewHomeVpAdapter(mutableListOf(searchComicFragment, searchNovelFragment), childFragmentManager, viewLifecycleOwner.lifecycle)  // 创建适配器
            binding.homeSearchVp.offscreenPageLimit = 2                                                                     // 设置预加载2页
            TabLayoutMediator(binding.homeSearchTablayout, binding.homeSearchVp) { tab, pos ->
                when(pos) {
                    0 -> { tab.text = getString(R.string.home_comic) }
                    1 -> { tab.text = getString(R.string.home_novel) }
                }
            }.attach()      // 关联VP和TabLayout
            editText.setOnEditorActionListener { _, _, event->                                                                  // 监听EditText 通知对应VP对应页发送意图
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    when(binding.homeSearchVp.currentItem) {
                        0 -> searchComicFragment.doInputSearchComicIntent()
                        1 -> searchNovelFragment.doInputSearchNovelIntent()
                    }
                }
                false
            }
        }
    }

    /** ● 获取ViewBinding */
    override fun getViewBinding(inflater: LayoutInflater) = HomeFragmentNewBinding.inflate(inflater)

    /** ● Lifecycle Start */
    override fun onStart() {
        super.onStart()
        mBackDispatcher = requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (mBinding.homeSearchView.isShowing) mBinding.homeSearchView.hide()
            else requireActivity().moveTaskToBack(true)
        }
    }

    /** ● Lifecycle Stop */
    override fun onStop() {
        super.onStop()
        mBaseEvent.remove(SEARCH_TAG)
    }

    /** ● Lifecycle Destroy */
    override fun onDestroyView() {
        super.onDestroyView()
        mRecRefresh = null  // 置空“换一批”控件 防止内存泄漏
        mSearchBinding = null
        parentFragmentManager.clearFragmentResultListener(HOME)
        mBaseEvent.remove("HOME_FRAGMENT_INIT_SEARCH_VIEW")
    }
    

    /** ● 初始化数据 */
    override fun initData(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mBaseEvent.remove("HOME_FRAGMENT_LOAD_HOME_PAGE")
            return
        }

        // 获取主页数据
        mHomeVM.input(HomeIntent.GetHomePage())

        // Refresh
        mBinding.homeRefresh.autoRefreshAnimationOnly()
    }

    /** ● 初始化视图  */
    override fun initView(savedInstanceState: Bundle?) {

        // 内存重启后隐藏SearchView
        if (savedInstanceState != null) {
            withLifecycle(state = Lifecycle.State.RESUMED) {
                mBinding.homeSearchView.hide()
            }
        }

        // 设置 内边距属性 实现沉浸式效果
        immersionPadding(mBinding.root) { view, insets, _ ->
            mBinding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = insets.top }
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
            }
        }

        // 设置刷新时不允许列表滚动
        mBinding.homeRefresh.setDisableContentWhenRefresh(true)

        // 设置适配器
        mBinding.homeRv.adapter = mDataRvAdapter

        // 设置加载动画独占1行，漫画卡片3行
        (mBinding.homeRv.layoutManager as GridLayoutManager).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when(mDataRvAdapter.getItemViewType(position)) {
                        NewHomeComicRvAdapter.HEADER -> spanCount
                        NewHomeComicRvAdapter.REFRESH -> spanCount
                        NewHomeComicRvAdapter.TOPIC -> spanCount
                        else -> 1
                    }
                }
            }
        }

        // 初始化Banner
        mBinding.homeComposeBanner.layoutParams.height = (resources.displayMetrics.widthPixels / 1.875 + 0.5).toInt()
//        val dp20 = resources.getDimensionPixelSize(baseR.dimen.base_dp20)
//        mBinding.homeBannerRv.layoutParams.height = (resources.displayMetrics.widthPixels / 1.875 + 0.5).toInt()
//        mBinding.homeBannerRv.addPageTransformer(ScaleInTransformer())
//            .setPageMargin(dp20, resources.getDimensionPixelSize(baseR.dimen.base_dp10))
//            .setIndicator(
//                IndicatorView(mBinding.homeBannerRv.context)
//                    .setIndicatorColor(Color.DKGRAY)
//                    .setIndicatorSelectorColor(Color.WHITE)
//                    .setIndicatorStyle(IndicatorView.IndicatorStyle.INDICATOR_BEZIER)
//                    .also{ it.setPadding(0, 0, 0, dp20) }
//            )
//        mBinding.homeBannerRv.adapter = mBannerRvAdapter
    }


    /** ● 初始化监听器 */
    override fun initListener() {

        // 设置容器Fragment的回调监听
        parentFragmentManager.setFragmentResultListener(HOME, this) { _, bundle ->
            if (bundle.getInt(BaseStrings.ID) == 0) {
                if (bundle.getBoolean(BaseStrings.ENABLE_DELAY)) {

                    launchDelay(BASE_ANIM_200L) { mHomeVM.input(HomeIntent.GetHomePage()) }
                }
                else mHomeVM.input(HomeIntent.GetHomePage())
            }
        }

        // 登录成功 监听
        parentFragmentManager.setFragmentResultListener(BaseEventEnum.LoginCategories.name, this) { _, bundle ->
            if (bundle.getInt(BaseStrings.ID) == 0) {
                mHomeVM.input(HomeIntent.GetHomePage())
            }
        }

        // 搜索
        mBinding.homeToolbar.menu[0].doOnClickInterval {
            if (mBinding.homeSearchView.isShowing) {
                mSearchBinding?.homeSearchVp?.let {  vp ->
                    when(vp.currentItem) {
                        0 -> { (childFragmentManager.fragments[0] as SearchComicFragment).doInputSearchComicIntent() }
                        1 -> { (childFragmentManager.fragments[1] as SearchNovelFragment).doInputSearchNovelIntent() }
                    }
                }
            } else {
                initSearchView()
                mBinding.homeSearchView.show()
            }
        }

        // 设置
        mBinding.homeToolbar.menu[1].doOnClickInterval { navigateSettings() }

        // MaterialToolBar NavigateIcon 点击事件
        mBinding.homeToolbar.navigateIconClickGap(flagTime = BaseEvent.BASE_FLAG_TIME_500 shl 1) {
            mBinding.homeRv.stopScroll()
            get<BottomSheetDialogFragment>(named(Fragments.User.name)).show(requireParentFragment().parentFragmentManager, null)
        }

        // 刷新
        mBinding.homeRefresh.setOnRefreshListener {
            mBaseEvent.remove("HOME_FRAGMENT_LOAD_HOME_PAGE")
            mHomeVM.input(HomeIntent.GetHomePage())
        }
    }

    /** ● 初始化监听器 */
    override fun initObserver(saveInstanceState: Bundle?) {

        mHomeVM.onOutput { intent ->
            when (intent) {

                // （获取主页）（根据 刷新事件 来决定是否启用加载动画） 正常加载数据、反馈View
                is HomeIntent.GetHomePage -> {
                    intent.mBaseViewState
                        .doOnSuccess {
                            if (mBinding.homeRefresh.isRefreshing) {
                                mBinding.homeRefresh.finishRefresh(BASE_ANIM_300L.toInt())
                                if (mBinding.homeTipsError.isGone)  toast(getString(baseR.string.BaseLoadingErrorNeedRefresh))
                            }
                        }
                        .doOnResult {
                            doLoadHomePage()
                        }
                        .doOnError { _, _ ->
                            if (mDataRvAdapter.itemCount == 0) {

                                // Banner 不可见
/*
                                mBinding.homeBannerRv.animateFadeOut().withEndAction {

                                    // Banner 消失
                                    mBinding.homeBannerRv.isGone = true

                                    // 错误提示淡入
                                    mBinding.homeTipsError.animateFadeIn()
                                }
*/
                                if (mBinding.homeComposeBanner.isVisible) {

                                    // Banner GONE
                                    mBinding.homeComposeBanner.animateFadeOut().withEndAction {

                                        // Banner 消失
                                        mBinding.homeComposeBanner.isGone = true

                                        // 错误提示淡入
                                        mBinding.homeTipsError.animateFadeIn()
                                    }
                                } else {

                                    // 错误提示淡入
                                    mBinding.homeTipsError.animateFadeIn()
                                }

                                // 发现页 “漫画” 淡出
                                mBinding.homeRv.animateFadeOutWithEndInVisibility()

                                // 取消刷新
                                mBinding.homeRefresh.finishRefresh()
                            }
                        }
                }

                // （刷新获取）不启用 加载动画 正常加载数据 -> 反馈View
                is HomeIntent.GetRecPageByRefresh -> {
                    intent.mBaseViewState
                        .doOnError { _, _ ->
                            toast(getString(baseR.string.BaseLoadingError))
                            mRecRefresh?.isEnabled = true
                        }
                        .doOnResult {

                            viewLifecycleOwner.lifecycleScope.launch {
                                async {
                                    mDataRvAdapter.onRefreshSubmitList(mHomeVM.getSnapshotHomeData(), 50L)
                                }.await()

                                mRecRefresh?.isEnabled = true
                            }
                        }
                }
            }
        }
    }
}