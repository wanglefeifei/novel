package sjj.novel.view.module.read

import androidx.lifecycle.ViewModel
import android.text.Html
import androidx.databinding.ObservableInt
import io.reactivex.Flowable
import io.reactivex.Observable
import sjj.novel.data.repository.novelDataRepository
import sjj.novel.model.Book
import sjj.novel.model.Chapter
import sjj.novel.util.lazyFromIterable
import sjj.novel.view.reader.page.TxtChapter
import java.util.concurrent.TimeUnit

class ReadViewModel(val name: String, val author: String) : ViewModel() {

    var chapterList:List<Chapter> = listOf()
    val book:Flowable<Book> = novelDataRepository.getBookInBookSource(name, author).flatMap { b ->
        novelDataRepository.getChapterIntro(b.url).map { chapters->
            b.chapterList = chapters
            chapterList = chapters
            b
        }
    }

    private var lastReadIndex = 0
    private var isThrough: Boolean? = null

    fun getChapters(bookUrl: String) = novelDataRepository.getChapters(bookUrl)

    fun loadChapter(chapter: Chapter): Observable<Chapter> {
        return novelDataRepository.loadChapter(chapter)
    }

    fun getChapter(url: String): Observable<Chapter> {
        return novelDataRepository.getChapter(url)
    }

    fun getChapter(requestChapters: List<TxtChapter>, force: Boolean = false): Observable<List<TxtChapter>> {
        return Observable.just(requestChapters).lazyFromIterable { txtChapter ->
            novelDataRepository.getChapter(txtChapter.link,force).map { chapter ->
                txtChapter.content = Html.fromHtml(chapter.content).toString()
                txtChapter.title = chapter.chapterName
                txtChapter
            }.delay(500, TimeUnit.MILLISECONDS)
        }.flatMap { it }.reduce(requestChapters) { _, _ -> requestChapters }.toObservable()
    }

    val readIndex = novelDataRepository.getBookSourceRecord(name, author).doOnNext {
        lastReadIndex = it.readIndex
    }

    fun setReadIndex(index: Chapter, pagePos: Int, isThrough: Boolean = false): Observable<Int> {
        if (lastReadIndex == index.index && this.isThrough == isThrough) {
            return Observable.empty()
        }
        this.isThrough = isThrough
        lastReadIndex = index.index
        return novelDataRepository.setReadIndex(name, author, index, pagePos, isThrough)
    }


    fun cachedBookChapter(bookUrl: String) = novelDataRepository.cachedBookChapter(bookUrl)

    fun refresh(): Observable<Book> {
        return book.firstElement().toObservable().flatMap {
            novelDataRepository.refreshBook(it.url)
        }
    }

}