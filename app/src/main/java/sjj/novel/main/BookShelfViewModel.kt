package sjj.novel.main

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import android.databinding.ObservableInt
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.functions.Function
import sjj.novel.R
import sjj.novel.data.repository.novelDataRepository
import sjj.novel.data.repository.novelSourceRepository
import sjj.novel.model.Book
import sjj.novel.util.host
import sjj.novel.util.lazyFromIterable
import sjj.novel.util.resStr
import java.util.concurrent.TimeUnit

class BookShelfViewModel : ViewModel() {
    val books:Flowable<List<BookShelfItemViewModel>> = novelDataRepository.getBooks().flatMap { list ->

        val map = list.map { book ->
            BookShelfItemViewModel().apply {
                this.book = book
                bookName.set(book.name)
                author.set(R.string.author_.resStr(book.author))
                bookCover.set(book.bookCoverImgUrl)
            }
        }

        Observable.fromIterable(map).flatMap {
            novelDataRepository.getLatestChapter(it.book.url).map {chapter->
                it.lastChapter.set(R.string.newest_.resStr(chapter.chapterName))
                it.book.lastChapter = chapter
                it
            }.switchIfEmpty(Observable.just(it))
        }.flatMap {model->
            novelDataRepository.getBookSourceRecord(model.book.name, model.book.author).firstElement().toObservable().map {
                model.book.index = it.readIndex
                model.book.isThrough = it.isThrough
                model.haveRead.set(R.string.haveRead_.resStr(it.chapterName))
                model
            }
        }.flatMap {model->
            novelSourceRepository.getBookParse(model.book.name,model.book.author).map {list->
                model.origin.set(R.string.origin_.resStr(list.find { model.book.url.host.endsWith(it.topLevelDomain) }?.sourceName,list.size))
                model
            }
        }.reduce(map) { r, model ->
            model.remainingChapter.set(maxOf((model.book.lastChapter?.index?:0) - model.book.index + (if (model.book.isThrough) 0 else 1), 0))
            r //保持顺序
        }.toFlowable()
    }

    /**
     *  刷新书籍书籍。
     */
    fun refresh(): Observable<Book> {
        return novelDataRepository.getBooks()
                .firstElement()
                .toObservable()
                .flatMap { list ->
                    list.forEach {
                        it.loadStatus = Book.LoadState.Loading
                    }
                    val disposed = list.toMutableList()
                    novelDataRepository.batchUpdate(list)
                            .flatMap { bookList ->
                                //对小说进行分组使同同一来源的小说一次性发射出去
                                val map = mutableMapOf<String, MutableList<Book>>()
                                bookList.forEach {
                                    map.getOrPut(it.url.host) { mutableListOf() }.add(it)
                                }
                                Observable.fromIterable(map.values)
                                        .lazyFromIterable { book ->
                                            novelDataRepository.refreshBook(book.url)
                                                    .delay(500, TimeUnit.MILLISECONDS)
                                                    .onErrorResumeNext(Function { _ ->
                                                        book.loadStatus = Book.LoadState.LoadFailed
                                                        novelDataRepository.batchUpdate(listOf(book)).map { it.first() }
                                                    }).doOnNext {
                                                        disposed.remove(it)
                                                    }
                                        }.flatMap { it }
                            }.doOnDispose {
                                list.forEach {
                                    it.loadStatus = Book.LoadState.UnLoad
                                }
                                //刷新被取消的时候将正在加载的数据改为未加载
                                novelDataRepository.batchUpdate(list).subscribe()
                            }
                }
    }


    fun delete(book: Book) {
        novelDataRepository.deleteBook(book.name, book.author)
                .subscribe()
    }

    class BookShelfItemViewModel {
        lateinit var book:Book
        val bookCover = ObservableField<String>()
        val bookName = ObservableField<String>()
        val author = ObservableField<String>()
        val lastChapter = ObservableField<String>()
        val haveRead = ObservableField<String>()
        val remainingChapter = ObservableInt()
        val origin = ObservableField<String>()

    }

}