package sjj.novel.view.fragment

import androidx.lifecycle.ViewModel
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.subjects.BehaviorSubject
import net.ricecode.similarity.JaroWinklerStrategy
import sjj.novel.R
import sjj.novel.data.repository.novelDataRepository
import sjj.novel.data.repository.novelSourceRepository
import sjj.novel.data.source.local.localFictionDataSource
import sjj.novel.model.Book
import sjj.novel.model.Chapter
import sjj.novel.util.host
import sjj.novel.util.id
import sjj.novel.util.resStr
import kotlin.math.abs

class ChooseBookSourceViewModel(val bookName: String, val bookAuthor: String) : ViewModel() {
    val isRefreshing = ObservableBoolean()

    var bookList = listOf<ChooseBookSourceItemViewModel>()

    fun fillViewModel(): Flowable<List<ChooseBookSourceItemViewModel>> {
        return localFictionDataSource.getBooks(bookName, bookAuthor).flatMap {
            bookList = it.map {
                val model = ChooseBookSourceItemViewModel()
                model.book = it
                model.bookCover.onNext(it.bookCoverImgUrl)
                model.bookName.set(it.name)
                model.author.set(R.string.author_.resStr(it.author))
                model.loading.set(it.loadStatus == Book.LoadState.Loading)
//               model.origin.set(R.string.origin.resStr(it.))
                model
            }
            novelSourceRepository.getAllBookParseRule().map { list ->
                bookList.forEach { model ->
                    val rule = list.find { rule -> model.book.url.host.endsWith(rule.topLevelDomain) }
                    model.origin.set(R.string.origin.resStr(rule?.sourceName))
                }
                bookList
            }.flatMap { modelList ->
                Flowable.fromIterable(modelList).flatMap { m ->
                    if (m.book.loadStatus == Book.LoadState.UnLoad) {
                        Flowable.just(m)
                    } else {
                        localFictionDataSource.getLatestChapter(m.book.url).doOnNext { chapter ->
                            m.lastChapter.set(R.string.newest_.resStr(chapter.chapterName))
                        }.toFlowable(BackpressureStrategy.BUFFER)
                    }
                }
            }
        }.map {
            bookList
        }
    }

    fun refresh(): Observable<Book> {
        isRefreshing.set(true)
        return localFictionDataSource.getBooks(bookName, bookAuthor).firstElement().toObservable().flatMap {
            Observable.fromIterable(it).flatMap {
                novelDataRepository.refreshBook(it.url)
            }
        }.doOnTerminate {
            isRefreshing.set(false)
        }
    }

    fun setBookSource(book: Book): Observable<Int> {
        return novelDataRepository.getBookSourceRecord(book.name, book.author).firstElement().toObservable().flatMap { b ->
            //当前阅读的章节名
            val chapterName = b.chapterName
            if (chapterName.isBlank()) {
                novelDataRepository.setBookSource(bookName, bookAuthor, book.url)
            } else {
                localFictionDataSource.getChapterIntro(book.url).firstElement().toObservable().flatMap {
                    val dig = JaroWinklerStrategy()
                    var temp = 100.0
                    val cs = mutableListOf<Chapter>()
                    for (i in 0 until it.size) {
                        //新来源中的书籍章节名
                        val name = it[i].chapterName
                        if (chapterName.length == name.length) {
                            if (chapterName == name) {
                                cs.clear()
                                cs.add(it[i])
                                break
                            }
                            continue
                        }
                        val r = dig.score(chapterName, name)
                        if (temp > r) {
                            temp = r
                            cs.clear()
                            cs.add(it[i])
                        } else if (temp == r) {
                            cs.add(it[i])
                        }
                    }
                    if (cs.isEmpty()) {
                        novelDataRepository.setBookSource(bookName, bookAuthor, book.url)
                    } else {
                        //这里有一点小问题 。 新来源的书籍可能没有当前的书源章节多。始终找不到最新书籍 可能会随机找上一个最相似的书籍章节。
                        //我认为作为一款免费的app这应当是可以忍受的
                        var t: Chapter? = null
                        cs.forEach { chapter ->
                            if (t == null) {
                                t = chapter
                            } else if (abs(chapter.index - b.readIndex) <= abs(t!!.index - b.readIndex)) {
                                t = chapter
                            }
                        }
                        novelDataRepository.setReadIndex(book.name, book.author, t!!, b.pagePos, b.isThrough).flatMap {
                            novelDataRepository.setBookSource(bookName, bookAuthor, book.url)
                        }
                    }
                }
            }
        }
//        return novelDataRepository.setBookSource(bookName, author, book.url)
    }

    class ChooseBookSourceItemViewModel {
        lateinit var book: Book
        val bookCover = BehaviorProcessor.create<String>()
        val bookName = ObservableField<String>()
        val author = ObservableField<String>()
        val lastChapter = ObservableField<String>()
        val origin = ObservableField<String>()
        val id: Long by lazy { book.url.id }
        val loading = ObservableBoolean()
    }
}