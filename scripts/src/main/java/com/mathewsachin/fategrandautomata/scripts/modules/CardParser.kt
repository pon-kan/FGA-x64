package com.mathewsachin.fategrandautomata.scripts.modules

import com.mathewsachin.fategrandautomata.scripts.IFgoAutomataApi
import com.mathewsachin.fategrandautomata.scripts.Images
import com.mathewsachin.fategrandautomata.scripts.ScriptNotify
import com.mathewsachin.fategrandautomata.scripts.enums.CardAffinityEnum
import com.mathewsachin.fategrandautomata.scripts.enums.CardTypeEnum
import com.mathewsachin.fategrandautomata.scripts.models.CommandCard
import com.mathewsachin.fategrandautomata.scripts.models.ParsedCard
import com.mathewsachin.libautomata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class CardParser @Inject constructor(
    fgAutomataApi: IFgoAutomataApi,
    private val servantTracker: ServantTracker
) : IFgoAutomataApi by fgAutomataApi {

    private fun CommandCard.Face.affinity(): CardAffinityEnum {
        val region = game.affinityRegion(this)

        if (images[Images.Weak] in region) {
            return CardAffinityEnum.Weak
        }

        if (images[Images.Resist] in region) {
            return CardAffinityEnum.Resist
        }

        return CardAffinityEnum.Normal
    }

    private fun CommandCard.Face.isStunned(): Boolean {
        val stunRegion = game.typeRegion(this).copy(
            y = 930,
            width = 248,
            height = 188
        )

        return images[Images.Stun] in stunRegion
    }

    private fun CommandCard.Face.type(): CardTypeEnum {
        val region = game.typeRegion(this)

        if (images[Images.Buster] in region) {
            return CardTypeEnum.Buster
        }

        if (images[Images.Arts] in region) {
            return CardTypeEnum.Arts
        }

        if (images[Images.Quick] in region) {
            return CardTypeEnum.Quick
        }

        return CardTypeEnum.Unknown
    }

    fun parse(): List<ParsedCard> {
        val cardsGroupedByServant = servantTracker.faceCardsGroupedByServant()

        val cards = CommandCard.Face.list
            .map {
                val stunned = it.isStunned()
                val type = if (stunned)
                    CardTypeEnum.Unknown
                else it.type()
                val affinity = if (type == CardTypeEnum.Unknown)
                    CardAffinityEnum.Normal // Couldn't detect card type, so don't care about affinity
                else it.affinity()

                ParsedCard(
                    card = it,
                    isStunned = stunned,
                    type = type,
                    affinity = affinity,
                    servant = cardsGroupedByServant
                        .filterValues { cards -> it in cards }
                        .keys
                        .first()
                )
            }

        val failedToDetermine = cards
            .filter { !it.isStunned && it.type == CardTypeEnum.Unknown }
            .map { it.card }

        if (failedToDetermine.isNotEmpty()) {
            messages.notify(
                ScriptNotify.FailedToDetermineCards(failedToDetermine)
            )
        }

        return cards
    }
}