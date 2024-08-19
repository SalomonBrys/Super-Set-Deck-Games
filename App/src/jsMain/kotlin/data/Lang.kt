package data

@Suppress("PropertyName")
data class Lang(
    val langName: String,
    val title_Games: String,
    val title_MyLists: String,
    val title_TravelPacker: String,
    val title_About: String,
    val title_Changelog: String,
    val X_players: (String) -> String,
    val X_games: (Int) -> String,
    val X_types: (Int) -> String,
    val With_X_variants: (Int) -> String,
    val filter__by_Name: String,
    val filter__by_Rating: String,
    val filter__by_Year: String,
    val filter__allTypes: String,
    val filter__searchPlaceholder: String,
    val game__A_game_by: String,
    val game__adapted_by: String,
    val game__A_traditional_game: (String) -> String,
    val share__Share_by_Message: String,
    val share__Share_by_QR_Code: String,
    val share__Copy_game_page_link: String,
    val lists__favourites: String,
    val lists__New_list: String,
    val lists__Add_to_list: String,
    val lists__Backup_Restore: String,
    val sync__Synchronize_with_local_files: String,
    val sync__Create_new_file: String,
    val sync__Open_existing_file: String,
    val sync__SSD_backup_file: String,
    val sync__Synchronize_with_Google_Drive: String,
    val sync__A_new_file_will_be_created_in_your_drive_AppData: String,
    val sync__Last_Google_Drive_backup: (String) -> String,
    val sync__Delete_Drive_backup: String,
    val sync__or: String,
    val sync__Application: String,
    val sync__Backup: String,
    val sync__Synchronize: String,
    val sync__Everything_is_up_to_date: String,
    val putListDialog__New_game_list: String,
    val putListDialog__Rename_list: (String) -> String,
    val putListDialog__List_name: String,
    val putListDialog__create: String,
    val putListDialog__rename: String,
    val deleteListDialog__delete_list: (String) -> String,
    val deleteListDialog__delete: String,
    val dialog__cancel: String,
    val Error_opening_database: (String) -> String,
    val Error_loading_games: (String) -> String,
    val Error_loading_game: String,
    val Error_loading_document: String,

    val gameTypes: Map<String, String> = emptyMap(),
    val traditions: Map<String, String> = emptyMap(),
    val cardNames: Map<String, String> = emptyMap(),
) {

    companion object : Map<String, Lang> by langs {
        val default = "en"
    }

}

private val langs = mapOf(

    "en" to Lang(
        langName = "English",
        title_Games = "Games",
        title_MyLists = "My Lists",
        title_TravelPacker = "Travel Packer",
        title_About = "About",
        title_Changelog = "Changelog",
        X_players = {
            when (it) {
                "0" -> "nobody"
                "1" -> "1 player"
                else -> "$it players"
            }
        },
        X_games = {
            when (it) {
                0 -> "no game"
                1 -> "1 game"
                else -> "$it games"
            }
        },
        X_types = {
            when (it) {
                0 -> "no type"
                1 -> "1 type"
                else -> "$it types"
            }
        },
        With_X_variants = {
            when (it) {
                0 -> "Without variant"
                1 -> "With 1 variant"
                else -> "With $it variants"
            }
        },
        filter__by_Name = "by Name",
        filter__by_Rating = "by BGG Rating",
        filter__by_Year = "by Publication Year",
        filter__allTypes = "all types",
        filter__searchPlaceholder = "Name / Author",
        game__A_game_by = "A game by",
        game__adapted_by = "adapted by",
        game__A_traditional_game = { "A traditional $it game" },
        share__Share_by_Message = "Share by Message",
        share__Share_by_QR_Code = "Share by QR-Code",
        share__Copy_game_page_link = "Copy Game page link",
        lists__favourites = "Favourites",
        lists__New_list = "New list",
        lists__Add_to_list = "Add to list",
        lists__Backup_Restore = "Backup / Restore",
        sync__Synchronize_with_local_files = "Synchronize with Local Files",
        sync__Create_new_file = "Create new file",
        sync__Open_existing_file = "Open existing file",
        sync__SSD_backup_file = "Super-Set-Deck backup file",
        sync__Synchronize_with_Google_Drive = "Synchronize with Google Drive",
        sync__A_new_file_will_be_created_in_your_drive_AppData = "A new file will be created in your Drive AppData.",
        sync__Last_Google_Drive_backup = { "Last Google Drive backup: $it." },
        sync__Delete_Drive_backup = "Delete Drive backup?",
        sync__or = "or",
        sync__Application = "Application",
        sync__Backup = "Backup",
        sync__Synchronize = "Synchronize",
        sync__Everything_is_up_to_date = "Everything is up to date!",
        putListDialog__New_game_list = "New Game List",
        putListDialog__Rename_list = { "Rename \"$it\"" },
        putListDialog__List_name = "List name",
        putListDialog__create = "Create",
        putListDialog__rename = "Rename",
        deleteListDialog__delete_list = { "Delete \"$it\"?" },
        deleteListDialog__delete = "Delete",
        dialog__cancel = "Cancel",
        Error_opening_database = { "Could not open database: $it. The app is in degraded mode, without any persistance." },
        Error_loading_games = { "Could load games list: $it." },
        Error_loading_game = "Could not load game documentation.",
        Error_loading_document = "Could not load document.",
    ),

    "fr" to Lang(
        langName = "Français",
        title_Games = "Jeux",
        title_MyLists = "Mes Listes",
        title_TravelPacker = "Packer",
        title_About = "À propos",
        title_Changelog = "Historique",
        X_players = {
            when (it) {
                "0" -> "personne"
                "1" -> "1 joueur·euse"
                else -> "$it joueur·euse·s"
            }
        },
        X_games = {
            when (it) {
                0 -> "pas de jeux"
                1 -> "1 jeu"
                else -> "$it jeux"
            }
        },
        X_types = {
            when (it) {
                0 -> "aucun type"
                1 -> "1 type"
                else -> "$it types"
            }
        },
        With_X_variants = {
            when (it) {
                0 -> "Sans variante"
                1 -> "Avec 1 variante"
                else -> "Avec $it variantes"
            }
        },
        filter__by_Name = "par Nom",
        filter__by_Rating = "par Note BGG",
        filter__by_Year = "par Année de Publication",
        filter__allTypes = "tous types",
        filter__searchPlaceholder = "Nom / Auteur·Autrice",
        game__A_game_by = "Un jeu de",
        game__adapted_by = "adapté par",
        game__A_traditional_game = { "Un jeu traditionnel $it" },
        share__Share_by_Message = "Partager par Message",
        share__Share_by_QR_Code = "Partager par QR-Code",
        share__Copy_game_page_link = "Copier le lien de la Page du jeu",
        lists__favourites = "Favoris",
        lists__New_list = "Nouvelle liste",
        lists__Add_to_list = "Ajouter à une liste",
        lists__Backup_Restore = "Sauvegarder / Restorer",
        sync__Synchronize_with_local_files = "Synchroniser avec un fichier local",
        sync__Create_new_file = "Creer un nouveau fichier",
        sync__Open_existing_file = "Ouvrir un fichier existant",
        sync__SSD_backup_file = "Fichier de backup Super-Set-Deck",
        sync__Synchronize_with_Google_Drive = "Synchroniser avec Google Drive",
        sync__A_new_file_will_be_created_in_your_drive_AppData = "Un nouveau fichier sera créé dans votre Drive AppData.",
        sync__Last_Google_Drive_backup = { "Dernier backup sur Google Drive : $it." },
        sync__Delete_Drive_backup = "Supprimer le backup sur Drive ?",
        sync__or = "ou",
        sync__Application = "Application",
        sync__Backup = "Backup",
        sync__Synchronize = "Synchroniser",
        sync__Everything_is_up_to_date = "Tout est à jour!",
        putListDialog__New_game_list = "Nouvelle Liste de Jeux",
        putListDialog__Rename_list = { "Renommer \"$it\"" },
        putListDialog__List_name = "Nom de la liste",
        putListDialog__create = "Créer",
        putListDialog__rename = "Renommer",
        deleteListDialog__delete_list = { "Supprimer \"$it\" ?" },
        deleteListDialog__delete = "Supprimer",
        dialog__cancel = "Annuler",
        Error_opening_database = { "Impossible d'accéder à la base de données : ($it). L'application est en mode dégradé sans persistance." },
        Error_loading_games = { "Impossible de charger la liste des jeux : $it." },
        Error_loading_game = "Impossible de charger la documentation du jeu.",
        Error_loading_document = "Impossible de charger le document.",

        gameTypes = mapOf(
            "Auction" to "Enchères",
            "Bluffing" to "Bluff",
            "Cooperative" to "Coopératif",
            "Deduction" to "Déduction",
            "Drafting" to "Draft",
            "Hand Management" to "Gestion de Main",
            "Ladder Climbing" to "Montée en Puissance",
            "Memory" to "Mémoire",
            "Player Elimination" to "Élimination de joueur·euse·s",
            "Predictive Bid" to "Pari Prédictif",
            "Push Your Luck" to "Stop ou Encore",
            "Score and Reset" to "Score et Recommence",
            "Set Collection" to "Collection d'ensembles",
            "Single Loser" to "Perdant Unique",
            "Team Based" to "En Équipe",
            "Trick Taking" to "Prise de pli",
        ),

        traditions = mapOf(
            "Chinese" to "Chinois",
            "French" to "Français",
        ),

        cardNames = mapOf(
            "spades" to "piques",
            "hearts" to "coeurs",
            "clubs" to "trèfles",
            "diamonds" to "carreaux",
            "florettes" to "fleurettes",
            "wheels" to "roues",
            "stars" to "étoiles",
            "specials" to "spéciales",

            "Butterfly" to "Papillon",
            "Wolf" to "Loup",
            "Phoenix" to "Phénix",
            "Snake" to "Serpent",
            "Dragon" to "Dragon",
            "Monkey" to "Singe",
        )
    )

)
