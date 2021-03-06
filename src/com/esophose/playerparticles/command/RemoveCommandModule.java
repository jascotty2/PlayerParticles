package com.esophose.playerparticles.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.bukkit.util.StringUtil;

import com.esophose.playerparticles.manager.DataManager;
import com.esophose.playerparticles.manager.LangManager;
import com.esophose.playerparticles.manager.LangManager.Lang;
import com.esophose.playerparticles.particles.PPlayer;
import com.esophose.playerparticles.particles.ParticleEffect;
import com.esophose.playerparticles.particles.ParticleGroup;
import com.esophose.playerparticles.particles.ParticlePair;
import com.esophose.playerparticles.styles.api.ParticleStyle;

public class RemoveCommandModule implements CommandModule {

    public void onCommandExecute(PPlayer pplayer, String[] args) {
        if (args.length == 0) {
            LangManager.sendMessage(pplayer, Lang.REMOVE_NO_ARGS);
            return;
        }
        
        if (StringUtils.isNumeric(args[0])) { // Removing by ID
            int id;
            try {
                id = Integer.parseInt(args[0]);
            } catch (Exception ex) {
                LangManager.sendMessage(pplayer, Lang.ID_INVALID);
                return;
            }

            if (id <= 0) {
                LangManager.sendMessage(pplayer, Lang.ID_INVALID);
                return;
            }

            boolean removed = false;
            ParticleGroup activeGroup = pplayer.getActiveParticleGroup();
            for (ParticlePair particle : activeGroup.getParticles()) {
                if (particle.getId() == id) {
                    activeGroup.getParticles().remove(particle);
                    removed = true;
                    break;
                }
            }

            if (!removed) {
                LangManager.sendMessage(pplayer, Lang.ID_UNKNOWN, id);
                return;
            }

            DataManager.saveParticleGroup(pplayer.getUniqueId(), activeGroup);
            LangManager.sendMessage(pplayer, Lang.REMOVE_ID_SUCCESS, id);
        } else { // Removing by effect/style name
            ParticleEffect effect = ParticleEffect.fromName(args[0]);
            ParticleStyle style = ParticleStyle.fromName(args[0]);
            
            if (effect != null) {
                int removedCount = 0;
                ParticleGroup activeGroup = pplayer.getActiveParticleGroup();
                for (int i = activeGroup.getParticles().size() - 1; i >= 0; i--) {
                    if (activeGroup.getParticles().get(i).getEffect() == effect) {
                        activeGroup.getParticles().remove(i);
                        removedCount++;
                    }
                }
                
                if (removedCount > 0) {
                    DataManager.saveParticleGroup(pplayer.getUniqueId(), activeGroup);
                    LangManager.sendMessage(pplayer, Lang.REMOVE_EFFECT_SUCCESS, removedCount, effect.getName());
                } else {
                    LangManager.sendMessage(pplayer, Lang.REMOVE_EFFECT_NONE, effect.getName());
                }
            } else if (style != null) {
                int removedCount = 0;
                ParticleGroup activeGroup = pplayer.getActiveParticleGroup();
                for (int i = activeGroup.getParticles().size() - 1; i >= 0; i--) {
                    if (activeGroup.getParticles().get(i).getStyle() == style) {
                        activeGroup.getParticles().remove(i);
                        removedCount++;
                    }
                }
                
                if (removedCount > 0) {
                    DataManager.saveParticleGroup(pplayer.getUniqueId(), activeGroup);
                    LangManager.sendMessage(pplayer, Lang.REMOVE_STYLE_SUCCESS, removedCount, style.getName());
                } else {
                    LangManager.sendMessage(pplayer, Lang.REMOVE_STYLE_NONE, style.getName());
                }
            } else {
                LangManager.sendMessage(pplayer, Lang.REMOVE_UNKNOWN, args[0]);
            }
        }
    }

    public List<String> onTabComplete(PPlayer pplayer, String[] args) {
        List<String> matches = new ArrayList<>();
        Set<String> removeBy = new HashSet<>();

        for (ParticlePair particle : pplayer.getActiveParticles()) {
            removeBy.add(String.valueOf(particle.getId()));
            removeBy.add(particle.getEffect().getName());
            removeBy.add(particle.getStyle().getName());
        }

        if (args.length == 0) return new ArrayList<>(removeBy);

        StringUtil.copyPartialMatches(args[0], removeBy, matches);
        return matches;
    }

    public String getName() {
        return "remove";
    }

    public Lang getDescription() {
        return Lang.COMMAND_DESCRIPTION_REMOVE;
    }

    public String getArguments() {
        return "<ID>|<effect>|<style>";
    }

    public boolean requiresEffects() {
        return true;
    }

}
