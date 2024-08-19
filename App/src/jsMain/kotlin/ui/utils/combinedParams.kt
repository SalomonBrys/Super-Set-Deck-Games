package ui.utils

import react.*
import react.router.dom.SetURLSearchParams
import react.router.dom.useSearchParams
import web.url.URLSearchParams


val CombinedParamsProvider = FC<PropsWithChildren> { props ->
    val (initialParams) = useSearchParams()
    val combinedParams = useMemo { initialParams }

    combinedParamsContext(combinedParams) {
        +props.children
    }
}

private val combinedParamsContext = createContext<URLSearchParams>()

fun useGamesCombineSearchParams(): js.array.JsTuple2<URLSearchParams, SetURLSearchParams> {
    val tupleSearchParams = useSearchParams()
    val params: URLSearchParams? = useContext(combinedParamsContext)

    if (params == null) return tupleSearchParams

    val (_, setSearchParams) = tupleSearchParams
    val setParams: SetURLSearchParams = { f, o ->
        setSearchParams({ f!!(params) }, o)
    }

    return arrayOf(params, setParams).unsafeCast<js.array.JsTuple2<URLSearchParams, SetURLSearchParams>>()
}
