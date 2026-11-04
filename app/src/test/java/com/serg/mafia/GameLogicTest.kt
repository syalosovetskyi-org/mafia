package com.serg.mafia

import com.serg.mafia.model.CountdownTimer
import com.serg.mafia.model.Faction
import com.serg.mafia.model.GameViewModel
import com.serg.mafia.model.Phase
import com.serg.mafia.model.Role
import com.serg.mafia.model.Setup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Логика ведущего проверяется здесь, а не «на глаз по коду»:
 * ночь считается один раз и необратимо, ошибка ломает всю партию.
 */
class GameLogicTest {

    private fun game(
        players: Int = 10,
        doctor: Boolean = true,
        sheriff: Boolean = true,
        butterfly: Boolean = false,
        maniac: Boolean = false,
    ): GameViewModel = GameViewModel().apply {
        updateSetup {
            Setup(
                playerCount = players,
                names = com.serg.mafia.model.defaultNames(players),
                withDoctor = doctor,
                withSheriff = sheriff,
                withButterfly = butterfly,
                withManiac = maniac,
            )
        }
        startDeal()
    }

    /** Прогоняет раздачу до конца — телефон «обошёл круг». */
    private fun GameViewModel.finishDeal() {
        repeat(s.players.size) { dealNext() }
    }

    private fun GameViewModel.finishIntro() {
        while (s.phase == Phase.INTRO_NIGHT) introNext()
    }

    private fun GameViewModel.finishSpeechRound() {
        var guard = 0
        while (s.speakerId != null && guard++ < 100) finishSpeech()
    }

    @Test
    fun `состав на 10 игроков сходится`() {
        val setup = Setup(playerCount = 10, withButterfly = true, withManiac = false)
        assertEquals(10, setup.assignedTotal)
        assertTrue(setup.isValid)
        assertEquals(3, setup.mafiaTotal)
        assertEquals(1, setup.roleCounts[Role.DON])
        assertEquals(2, setup.roleCounts[Role.MAFIA])
        assertEquals(1, setup.roleCounts[Role.BUTTERFLY])
    }

    @Test
    fun `перебор спец-ролей не даёт начать партию`() {
        val setup = Setup(playerCount = 4, withButterfly = true, withManiac = true)
        assertFalse(setup.isValid)
        assertEquals("err_too_many_roles", setup.validationKey!!.first)
    }

    @Test
    fun `раздача выдаёт ровно заявленный состав`() {
        val vm = game(players = 12, butterfly = true, maniac = true)
        val counts = vm.s.players.groupingBy { it.role }.eachCount()
        assertEquals(vm.s.setup.roleCounts, counts)
        assertEquals(12, vm.s.players.size)
    }

    @Test
    fun `знакомство идёт по фракциям и заканчивается речами`() {
        val vm = game(butterfly = true, maniac = true)
        vm.finishDeal()
        assertEquals(Phase.INTRO_NIGHT, vm.s.phase)
        // мафия, дон, бабочка, врач, комиссар, маньяк
        assertEquals(6, vm.s.introSteps.size)
        assertEquals(
            vm.s.players.count { it.role.isBlack },
            vm.s.introActors(vm.s.introSteps[0]).size,
        )
        vm.finishIntro()
        assertEquals(Phase.INTRO_DAY, vm.s.phase)
        assertEquals(0, vm.s.speakerId)
    }

    @Test
    fun `после круга знакомства начинается первая ночь`() {
        val vm = game()
        vm.finishDeal()
        vm.finishIntro()
        vm.finishSpeechRound()
        assertEquals(Phase.NIGHT, vm.s.phase)
        assertEquals(1, vm.s.nightNumber)
    }

    @Test
    fun `порядок ночи бабочка мафия дон врач комиссар маньяк`() {
        val vm = game(players = 12, butterfly = true, maniac = true)
        vm.finishDeal()
        vm.finishIntro()
        vm.finishSpeechRound()
        assertEquals(
            listOf(Role.BUTTERFLY, Role.MAFIA, Role.DON, Role.DOCTOR, Role.SHERIFF, Role.MANIAC),
            vm.s.nightSteps.map { it.role },
        )
    }

    @Test
    fun `врач спасает жертву мафии`() {
        val vm = game()
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        val victim = vm.s.players.first { it.role == Role.CIVILIAN }
        vm.chooseNightTarget(Role.MAFIA, victim.id)
        vm.chooseNightTarget(Role.DOCTOR, victim.id)
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        assertEquals(Phase.MORNING, vm.s.phase)
        assertTrue(vm.s.lastNight.killed.isEmpty())
        assertTrue(vm.s.player(victim.id).alive)
        assertEquals(victim.id, vm.s.lastNight.savedByDoctor)
    }

    @Test
    fun `без врача жертва мафии погибает`() {
        val vm = game(doctor = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        val victim = vm.s.players.first { it.role == Role.CIVILIAN }
        vm.chooseNightTarget(Role.MAFIA, victim.id)
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        assertEquals(listOf(victim.id), vm.s.lastNight.killed)
        assertFalse(vm.s.player(victim.id).alive)
    }

    @Test
    fun `бабочка гасит проверку комиссара`() {
        val vm = game(butterfly = true, players = 12)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        val sheriff = vm.s.players.first { it.role == Role.SHERIFF }
        val someone = vm.s.players.first { it.role == Role.MAFIA }
        vm.chooseNightTarget(Role.BUTTERFLY, sheriff.id)
        vm.chooseNightTarget(Role.SHERIFF, someone.id)
        assertTrue(vm.isBlocked(sheriff.id))
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        assertNull(vm.s.lastNight.sheriffResult)
        assertEquals(sheriff.id, vm.s.lastNight.blocked)
    }

    @Test
    fun `первая ночь в воздух не убивает никого`() {
        val vm = game(doctor = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        vm.setFirstNightMiss(true)
        vm.chooseNightTarget(Role.MAFIA, vm.s.players.first { it.role == Role.CIVILIAN }.id)
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        assertTrue(vm.s.lastNight.killed.isEmpty())
    }

    @Test
    fun `настройка выстрела в воздух убирает жертву первой ночи`() {
        val vm = GameViewModel().apply {
            updateSetup {
                Setup(
                    playerCount = 10,
                    names = com.serg.mafia.model.defaultNames(10),
                    withDoctor = false,
                    firstNightMiss = true,
                )
            }
            startDeal()
        }
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        assertTrue(vm.s.night.mafiaMissed)
        vm.chooseNightTarget(Role.MAFIA, vm.s.players.first { it.role == Role.CIVILIAN }.id)
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        assertTrue(vm.s.lastNight.killed.isEmpty())
        // Вторая ночь уже обычная: промах не повторяется.
        vm.startDay(); vm.finishSpeechRound()
        assertFalse(vm.s.night.mafiaMissed)
    }

    @Test
    fun `ручное число мафии меняет состав`() {
        val setup = Setup(playerCount = 12, mafiaOverride = 5)
        assertEquals(5, setup.mafiaTotal)
        assertEquals(4, setup.roleCounts[Role.MAFIA])
        assertEquals(1, setup.roleCounts[Role.DON])
        assertTrue(setup.isValid)
        // Больше половины стола чёрными сделать нельзя.
        assertEquals(setup.maxMafia, Setup(playerCount = 12, mafiaOverride = 99).mafiaTotal)
    }

    @Test
    fun `возврат к настройкам сохраняет стол и сбрасывает партию`() {
        val vm = game(players = 8)
        vm.finishDeal()
        vm.backToSetup()
        assertEquals(Phase.SETUP, vm.s.phase)
        assertTrue(vm.s.players.isEmpty())
        assertEquals(8, vm.s.setup.playerCount)
    }

    @Test
    fun `перекос состава предлагает выстрел в воздух`() {
        assertTrue(Setup(playerCount = 6, withManiac = true).suggestsFirstNightMiss)
        assertFalse(Setup(playerCount = 15).suggestsFirstNightMiss)
    }

    @Test
    fun `два фола снимают одну речь и сгорают`() {
        val vm = game()
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val victimOfFouls = vm.s.alivePlayers.last()
        vm.addFoul(victimOfFouls.id)
        vm.addFoul(victimOfFouls.id)
        assertTrue(vm.s.player(victimOfFouls.id).speechSkipPending)

        vm.finishSpeechRound()
        // Речь пропущена и флаг сгорел — на следующий день игрок говорит.
        assertFalse(vm.s.player(victimOfFouls.id).speechSkipPending)
        assertTrue(vm.s.log.any { it.key == "log_skip_speech" })
    }

    @Test
    fun `третий фол выводит игрока из игры`() {
        val vm = game()
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val target = vm.s.alivePlayers.first { it.role == Role.CIVILIAN }
        repeat(3) { vm.addFoul(target.id) }
        assertFalse(vm.s.player(target.id).alive)
        assertEquals("death_fouls", vm.s.player(target.id).deathReasonKey)
    }

    @Test
    fun `выставления пишутся по каждому игроку, кандидат не дублируется`() {
        val vm = game()
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val alive = vm.s.alivePlayers
        vm.nominate(alive[0].id, alive[3].id)
        vm.nominate(alive[1].id, alive[3].id)
        // Двое выставили одного: кандидат один, а выставивших двое.
        assertEquals(listOf(alive[3].id), vm.s.candidates)
        assertEquals(setOf(alive[0].id, alive[1].id), vm.s.nominatedBy(alive[3].id).toSet())

        // Игрок передумал — его выставление снимается, кандидат остаётся от второго.
        vm.clearNomination(alive[0].id)
        assertEquals(listOf(alive[1].id), vm.s.nominatedBy(alive[3].id))

        // Смена цели заменяет прежний выбор, а не добавляет второй.
        vm.nominate(alive[1].id, alive[4].id)
        assertEquals(listOf(alive[4].id), vm.s.candidates)
    }

    @Test
    fun `день без выставленных уходит сразу в ночь`() {
        val vm = game(doctor = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        vm.finishSpeechRound()
        assertEquals(Phase.NIGHT, vm.s.phase)
        assertEquals(2, vm.s.nightNumber)
    }

    @Test
    fun `голосование выгоняет лидера и ведёт в ночь`() {
        val vm = game()
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val alive = vm.s.alivePlayers
        val candidate = alive.first { it.role == Role.CIVILIAN }
        vm.nominate(alive[0].id, candidate.id)
        vm.finishSpeechRound()
        assertEquals(Phase.VOTE, vm.s.phase)
        vm.setVote(candidate.id, 5)
        assertEquals(listOf(candidate.id), vm.voteLeaders)
        vm.eliminate(candidate.id)
        assertFalse(vm.s.player(candidate.id).alive)
        assertEquals(Phase.NIGHT, vm.s.phase)
    }

    @Test
    fun `город побеждает когда чёрных не осталось`() {
        val vm = game(players = 6, doctor = false, sheriff = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val blacks = vm.s.players.filter { it.role.isBlack }
        blacks.forEach { b ->
            if (vm.s.winner == null) {
                vm.nominate(vm.s.alivePlayers.first { !it.role.isBlack }.id, b.id)
                vm.setVote(b.id, 3)
                vm.eliminate(b.id)
                if (vm.s.phase == Phase.NIGHT) {
                    repeat(vm.s.nightSteps.size) { vm.nightNext() }
                    if (vm.s.phase == Phase.MORNING) vm.startDay()
                }
            }
        }
        assertEquals(Faction.RED, vm.s.winner)
        assertEquals(Phase.GAME_OVER, vm.s.phase)
    }

    @Test
    fun `мафия побеждает при равенстве`() {
        val vm = game(players = 6, doctor = false, sheriff = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        // Ночь за ночью мафия отстреливает мирных, пока не сравняется.
        var guard = 0
        while (vm.s.winner == null && guard++ < 20) {
            if (vm.s.phase == Phase.NIGHT) {
                val victim = vm.s.alivePlayers.firstOrNull { !it.role.isBlack }
                vm.chooseNightTarget(Role.MAFIA, victim?.id)
                repeat(vm.s.nightSteps.size) { vm.nightNext() }
            }
            if (vm.s.phase == Phase.MORNING) vm.startDay()
            if (vm.s.phase == Phase.DAY) vm.finishSpeechRound()
        }
        assertEquals(Faction.BLACK, vm.s.winner)
    }

    @Test
    fun `третий фол последнему говорящему переводит фазу и не софтлочит игру`() {
        val vm = game(players = 6, doctor = false, sheriff = false)
        vm.finishDeal(); vm.finishIntro()
        // В INTRO_DAY доходим до последнего говорящего
        while (vm.s.speechQueue().size > 1) {
            vm.finishSpeech()
        }
        val lastSpeaker = vm.s.speakerId!!
        assertEquals(1, vm.s.speechQueue().size)
        // Выдаём 3 фола последнему спикеру
        vm.addFoul(lastSpeaker)
        vm.addFoul(lastSpeaker)
        vm.addFoul(lastSpeaker)
        // Игрок должен выбыть, а фаза должна автоматически смениться на NIGHT (для intro_day)
        assertFalse(vm.s.player(lastSpeaker).alive)
        assertEquals(Phase.NIGHT, vm.s.phase)
    }

    @Test
    fun `третий фол последнему говорящему в день с выставлениями переводит в голосование`() {
        val vm = game(players = 6, doctor = false, sheriff = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        // Ночь без жертв
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val firstSpeaker = vm.s.speakerId!!
        val nominee = vm.s.alivePlayers.first { it.id != firstSpeaker }
        vm.nominate(firstSpeaker, nominee.id)
        // Доходим до последнего говорящего
        while (vm.s.speechQueue().size > 1) {
            vm.finishSpeech()
        }
        val lastSpeaker = vm.s.speakerId!!
        vm.addFoul(lastSpeaker); vm.addFoul(lastSpeaker); vm.addFoul(lastSpeaker)
        assertFalse(vm.s.player(lastSpeaker).alive)
        // Круг закрыт, есть выставленные -> должен начаться VOTE
        assertEquals(Phase.VOTE, vm.s.phase)
    }

    @Test
    fun `третий фол кандидату во время голосования убирает его из голосов и лидеров`() {
        val vm = game(players = 6, doctor = false, sheriff = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val c1 = vm.s.alivePlayers[0].id
        val c2 = vm.s.alivePlayers[1].id
        vm.nominate(c1, c1)
        vm.nominate(c2, c2)
        vm.finishSpeechRound()
        assertEquals(Phase.VOTE, vm.s.phase)
        vm.setVote(c1, 4)
        vm.setVote(c2, 2)
        assertEquals(listOf(c1), vm.voteLeaders)
        // Кандидат c1 получает 3 фола
        vm.addFoul(c1); vm.addFoul(c1); vm.addFoul(c1)
        assertFalse(vm.s.player(c1).alive)
        // c1 должен быть удалён из votes и не быть лидером голосования
        assertFalse(vm.s.votes.containsKey(c1))
        assertEquals(listOf(c2), vm.voteLeaders)
    }

    @Test
    fun `одновременная гибель всех игроков не объявляет победу города`() {
        val vm = game(players = 4, doctor = false, sheriff = false, maniac = true)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        val black = vm.s.players.first { it.role.isBlack }.id
        val maniac = vm.s.players.first { it.role == Role.MANIAC }.id
        val reds = vm.s.players.filter { !it.role.isBlack && it.role != Role.MANIAC }
        // Устраняем всех мирных через фолы
        reds.forEach {
            vm.addFoul(it.id); vm.addFoul(it.id); vm.addFoul(it.id)
        }
        // Мафия и маньяк стреляют друг в друга
        vm.chooseNightTarget(Role.MAFIA, maniac)
        vm.chooseNightTarget(Role.MANIAC, black)
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        assertEquals(0, vm.s.alivePlayers.size)
        assertNull("Город не может победить, когда все мирные мертвы", vm.s.winner)
        assertEquals(Phase.GAME_OVER, vm.s.phase)
    }

    @Test
    fun `один на один мафия и маньяк не завершают игру победой мафии`() {
        val vm = game(players = 4, doctor = false, sheriff = false, maniac = true)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        val reds = vm.s.players.filter { !it.role.isBlack && it.role != Role.MANIAC }
        reds.forEach {
            vm.addFoul(it.id); vm.addFoul(it.id); vm.addFoul(it.id)
        }
        assertEquals(2, vm.s.alivePlayers.size)
        assertNull("При 1 мафии и 1 маньяке мафия не имеет большинства", vm.s.winner)
    }

    @Test
    fun `таймер речи стартует, тикает и корректно останавливается`() {
        var fakeTime = 1000L
        var finished = false
        val timer = CountdownTimer(
            totalSeconds = 30,
            clock = { fakeTime },
            onFinished = { finished = true },
        )
        assertEquals(30, timer.remainingSeconds)
        assertFalse(timer.isRunning)

        timer.start()
        assertTrue(timer.isRunning)

        // Прошло 500мс — тика ещё нет
        fakeTime += 500
        assertFalse(timer.tick())
        assertEquals(30, timer.remainingSeconds)

        // Прошла 1 секунда
        fakeTime += 500
        assertTrue(timer.tick())
        assertEquals(29, timer.remainingSeconds)

        // Прошло сразу 10 секунд
        fakeTime += 10000
        assertTrue(timer.tick())
        assertEquals(19, timer.remainingSeconds)

        // Пауза
        timer.pause()
        assertFalse(timer.isRunning)
        fakeTime += 5000
        assertFalse(timer.tick())
        assertEquals(19, timer.remainingSeconds)

        // Доматываем до конца
        timer.start()
        fakeTime += 20000
        assertTrue(timer.tick())
        assertEquals(0, timer.remainingSeconds)
        assertFalse(timer.isRunning)
        assertTrue(timer.isFinished)
        assertTrue(finished)

        // Сброс
        timer.reset()
        assertEquals(30, timer.remainingSeconds)
        assertFalse(timer.isRunning)
        assertFalse(timer.isFinished)
    }

    @Test
    fun `партия на 4 игрока с минимальным составом валидна`() {
        val setup = Setup(
            playerCount = 4,
            withDoctor = false,
            withSheriff = false,
            withButterfly = false,
            withManiac = false,
            withDon = false,
        )
        assertTrue(setup.isValid)
        assertEquals(1, setup.mafiaTotal)
        assertEquals(3, setup.roleCounts[Role.CIVILIAN])
    }

    @Test
    fun `партия на 20 игроков с максимальным составом валидна`() {
        val setup = Setup(
            playerCount = 20,
            withDoctor = true,
            withSheriff = true,
            withButterfly = true,
            withManiac = true,
            withDon = true,
        )
        assertTrue(setup.isValid)
        assertEquals(7, setup.mafiaTotal)
        assertEquals(1, setup.roleCounts[Role.DON])
        assertEquals(6, setup.roleCounts[Role.MAFIA])
        assertEquals(1, setup.roleCounts[Role.DOCTOR])
        assertEquals(1, setup.roleCounts[Role.SHERIFF])
        assertEquals(1, setup.roleCounts[Role.BUTTERFLY])
        assertEquals(1, setup.roleCounts[Role.MANIAC])
        assertEquals(9, setup.roleCounts[Role.CIVILIAN])
    }

    @Test
    fun `партия без спецролей корректно формирует шаги ночи`() {
        val vm = game(players = 6, doctor = false, sheriff = false, butterfly = false, maniac = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        assertEquals(listOf(Role.MAFIA), vm.s.nightSteps.map { it.role })
    }

    @Test
    fun `ничья на голосовании отображает нескольких лидеров`() {
        val vm = game(players = 6, doctor = false, sheriff = false)
        vm.finishDeal(); vm.finishIntro(); vm.finishSpeechRound()
        repeat(vm.s.nightSteps.size) { vm.nightNext() }
        vm.startDay()
        val c1 = vm.s.alivePlayers[0].id
        val c2 = vm.s.alivePlayers[1].id
        vm.nominate(c1, c1)
        vm.nominate(c2, c2)
        vm.finishSpeechRound()
        vm.setVote(c1, 2)
        vm.setVote(c2, 2)
        assertEquals(2, vm.voteLeaders.size)
        assertTrue(vm.voteLeaders.contains(c1))
        assertTrue(vm.voteLeaders.contains(c2))
        vm.skipVote()
        assertEquals(Phase.NIGHT, vm.s.phase)
    }
}
