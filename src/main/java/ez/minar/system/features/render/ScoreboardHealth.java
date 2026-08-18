package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.UpdateEvent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

@NewFunction(name = "ScoreboardHealth", desc = "Берет здоровье из скорборда", category = Category.MISC)
public class ScoreboardHealth extends Function {

    public float RealHp;

    public ScoreboardHealth() {
    }

    public float getRealHp() {
        return RealHp;
    }

    @EventHandler
    public void onUpdate(UpdateEvent e) {
        if (mc.world == null) return;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            if (scoreboard == null) continue;

            ScoreboardObjective scoreobjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (scoreobjective != null) {
                ReadableScoreboardScore score2 = scoreboard.getScore(player, scoreobjective);
                if (score2 != null) {
                    String scoreText = score2.getScore() + " " + scoreobjective.getDisplayName().getString();
                    String scoreNumber = scoreText.replaceAll("[^0-9]", "");
                    try {
                        if (!scoreNumber.isEmpty()) {
                            int hps = Integer.parseInt(scoreNumber);
                            if (hps <= player.getMaxHealth()) {
                                RealHp = (float) hps;
                            }
                        }
                    } catch (NumberFormatException d) {
                    }
                }
            }
        }
    }
}
