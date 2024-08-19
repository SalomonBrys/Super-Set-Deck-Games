package module.react.image_gallery

import react.Props
import react.PropsWithRef
import react.ReactNode


external interface ImageGalleryProps : Props {
    var items: Array<Image>
    var showNav: Boolean?
    var showThumbnails: Boolean?
    var thumbnailPosition: String?
    var showPlayButton: Boolean?
    var showFullscreenButton: Boolean?
    var showBullets: Boolean?
    var startIndex: Int?
    var renderCustomControls: () -> ReactNode
    var renderFullscreenButton: (() -> Unit, Boolean) -> ReactNode

    interface Image {
        var original: String
        var thumbnail: String?
    }

}
