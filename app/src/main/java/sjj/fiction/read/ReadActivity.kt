package sjj.fiction.read

import android.app.ProgressDialog
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import com.raizlabs.android.dbflow.kotlinextensions.save
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_read.*
import org.jetbrains.anko.*
import sjj.alog.Log
import sjj.fiction.BaseActivity
import sjj.fiction.R
import sjj.fiction.data.Repository.FictionDataRepository
import sjj.fiction.model.Book
import sjj.fiction.model.BookGroup
import sjj.fiction.model.Chapter
import sjj.fiction.util.fictionDataRepository
import sjj.fiction.util.lparams
import sjj.fiction.util.textView
import sjj.fiction.util.toDpx

class ReadActivity : BaseActivity(), ReadContract.View {
    companion object {
        val DATA_BOOK_NAME = "DATA_BOOK_NAME"
        val DATA_BOOK_AUTHOR = "DATA_BOOK_AUTHOR"
        val DATA_CHAPTER_INDEX = "DATA_CHAPTER_INDEX"
    }

    private lateinit var presenter: ReadContract.Presenter

    private val bookName by lazy { intent.getStringExtra(DATA_BOOK_NAME) }
    private val bookAuthor by lazy { intent.getStringExtra(DATA_BOOK_AUTHOR) }
    private val index by lazy { intent.getIntExtra(DATA_CHAPTER_INDEX, 0) }
    private val contentAdapter by lazy { ChapterContentAdapter(presenter) }
    private val chapterListAdapter by lazy { ChapterListAdapter(presenter) }
    private var loadBookHint: ProgressDialog? = null
    private var cached: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        setSupportActionBar(toolbar)
        val supportActionBar = supportActionBar!!
        supportActionBar.setDisplayHomeAsUpEnabled(true)

        ReadPresenter(bookName, bookAuthor, index, this)

        chapterContent.layoutManager = LinearLayoutManager(this)
        chapterContent.adapter = contentAdapter

        chapterList.layoutManager = LinearLayoutManager(this)
        chapterList.adapter = chapterListAdapter
        chapterContent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val manager = chapterContent.layoutManager as LinearLayoutManager
                val position = manager.findFirstVisibleItemPosition()
                presenter.onContentScrolled(position)
            }
        })
        presenter.start()
    }

    override fun onStop() {
        super.onStop()
        presenter.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_read_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_cached -> {
                presenter.cachedBookChapter()
//                val dialog = progressDialog("正在加载书籍信息")
//                fictionDataRepository.loadBookGroup(bookName, bookAuthor).observeOn(AndroidSchedulers.mainThread())
//                        .subscribe({
//                            dialog.max = it.currentBook.chapterList.size
//                            dialog.setMessage("正在缓存章节类容……")
//                            fictionDataRepository.cachedBookChapter(it.currentBook)
//                                    .observeOn(AndroidSchedulers.mainThread())
//                                    .subscribe({
//                                        dialog.progress = dialog.progress + 1
//                                    }, {
//                                        toast("缓存出错")
//                                    }, {
//                                        dialog.dismiss()
//                                        toast("加载完成：${dialog.progress}")
//                                    }).also { com.add(it) }
//                        }, {
//                            dialog.dismiss()
//                            toast("书籍信息加载失败：${it.message}")
//                        }).also { com.add(it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setPresenter(presenter: ReadContract.Presenter) {
        this.presenter = presenter
    }

    override fun setChapterList(chapter: List<Chapter>) {
        chapterListAdapter.data = chapter
        contentAdapter.chapters = chapter
        chapterListAdapter.notifyDataSetChanged()
        contentAdapter.notifyDataSetChanged()
    }

    override fun setChapterListPosition(position: Int) {
        chapterList.scrollToPosition(position)
    }

    override fun setChapterContentPosition(position: Int) {
        chapterContent.scrollToPosition(position)
    }

    override fun setTitle(title: String) {
        this.title = title
    }

    override fun setLoadBookHint(active: Boolean) {
        loadBookHint = if (active) {
            loadBookHint ?: indeterminateProgressDialog("正在加载书籍请稍候……")
        } else {
            loadBookHint?.dismiss()
            null
        }
    }

    override fun setLoadBookHintError(e: Throwable) {
        toast("加载书籍出错：${e.message}")
    }

    override fun setChapterName(name: String) {
        chapterName.text = name
    }

    override fun notifyChapterContentChange() {
        contentAdapter.notifyDataSetChanged()
    }

    override fun setCachedBookChapterHint(active: Boolean) {
        cached = if (active) {
            cached ?: progressDialog("正在缓存章节内容")
        } else {
            cached?.dismiss()
            null
        }
    }

    override fun setCachedBookChapterProgressPlus(max: Int) {
        val dialog = cached ?: return
        if (dialog.max != max) {
            dialog.max = max
        }
        dialog.progress = dialog.progress + 1
    }

    override fun setCachedBookChapterComplete() {
        toast("缓存章节完成")
    }

    override fun setCachedBookChapterError(e: Throwable) {
        toast("缓存章节出错：${e.message}")
    }

    private class ChapterContentAdapter(val presenter: ReadContract.Presenter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var chapters: List<Chapter>? = null
        private val fiction: FictionDataRepository = fictionDataRepository
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(with(parent.context) {
                verticalLayout {
                    textView {
                        id = R.id.readItemChapterContentTitle
                        setPadding(16.toDpx(), 8.toDpx(), 16.toDpx(), 8.toDpx())
                        textSize = 24f
                        textColor = getColor(R.color.material_textBlack_text)
                    }
                    textView {
                        id = R.id.readItemChapterContent
                        setPadding(16.toDpx(), 8.toDpx(), 16.toDpx(), 8.toDpx())
                        textSize = 20f
                        textColor = getColor(R.color.material_textBlack_text)
                    }
                }.lparams<RecyclerView.LayoutParams, LinearLayout> {
                    width = RecyclerView.LayoutParams.MATCH_PARENT
                    height = RecyclerView.LayoutParams.MATCH_PARENT
                }
            }) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val chapter = chapters!![position]
            holder.itemView.findViewById<TextView>(R.id.readItemChapterContentTitle).text = chapter.chapterName
            if (chapter.content.isNotEmpty()) {
                holder.itemView.findViewById<TextView>(R.id.readItemChapterContent).text = Html.fromHtml(chapter.content)
            }
            if (!chapter.isLoadSuccess||chapter.content.isEmpty()) {
                presenter.setChapterContent(position)
                holder.itemView.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                holder.itemView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }

        override fun getItemCount(): Int = chapters?.size ?: 0
    }

    private class ChapterListAdapter(private val presenter: ReadContract.Presenter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var data: List<Chapter>? = null
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_text_text, parent, false)) {}
        }

        override fun getItemCount() = data?.size ?: 0

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.find<TextView>(R.id.text1).text = data!![position].chapterName
            holder.itemView.setOnClickListener {
                presenter.onSelectChapter(position)
            }
        }

    }
}
