package com.github.shynixn.blockball.bukkit.logic.business.entity.game;
/*
import com.github.shynixn.blockball.api.entities.*;
import com.github.shynixn.blockball.api.events.GameJoinEvent;
import com.github.shynixn.blockball.api.events.GameWinEvent;
import com.github.shynixn.blockball.api.persistence.entity.meta.effect.SoundEffectMeta;
import com.github.shynixn.blockball.bukkit.BlockBallPlugin;
import com.github.shynixn.blockball.bukkit.logic.business.configuration.ConfigOld;
import com.github.shynixn.blockball.bukkit.logic.business.configuration.Language;
import com.github.shynixn.blockball.bukkit.logic.business.entity.TemporaryPlayerStorage;
import com.github.shynixn.blockball.bukkit.nms.NMSRegistry;
import com.github.shynixn.blockball.bukkit.logic.persistence.entity.meta.effect.SoundBuilder;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HelperGameEntity extends GameEntity implements MiniGame {
    private final List<Player> lobby = new ArrayList<>();
    private GameStage stage = GameStage.DISABLED;
    private int countdown;
    private boolean isStarting;
    private final Map<Player, Team> preSelection = new HashMap<>();
    private final SoundEffectMeta blingsound = new SoundBuilder("NOTE_PLING", 1.0, 2.0);

    private int bumeper = 40;
    private int secondbumper = 20;
    private final Map<Player, Integer> bumpers = new HashMap<>();

    HelperGameEntity(Arena arena) {
        super(arena);
        if (arena.isEnabled())
            this.stage = GameStage.ENABLED;
        this.countdown = arena.getLobbyMeta().getCountDown();
    }

    @Override
    public synchronized boolean leave(Player player) {
        if (this.lobby.contains(player))
            this.lobby.remove(player);
        if (this.preSelection.containsKey(player))
            this.preSelection.remove(player);
        return super.leave(player);
    }

    public synchronized boolean joinLobby(Player player) {
        if (this.canJoinLobby(player)) {
            this.lobby.add(player);
            this.storeTemporaryInventory(player);
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().clear();
            player.updateInventory();
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setExp(0);
            player.setLevel(0);
            player.setGameMode(GameMode.ADVENTURE);
            player.teleport(this.getArena().getLobbyMeta().getLobbySpawn());
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean join(Player player, Team team) {
        if (team == Team.RED) {
            this.redTeam.add(player);
            if (this.arena.getTeamMeta().getRedSpawnPoint() != null)
                player.teleport(this.arena.getTeamMeta().getRedSpawnPoint());
            else
                player.teleport(this.arena.getBallSpawnLocation());
            if (this.getHologram() != null) {
                this.getHologram().show(player);
            }
            this.addPlayerToScoreboard(player);
            Bukkit.getPluginManager().callEvent(new GameJoinEvent(this, player));
            return true;
        } else if (team == Team.BLUE) {
            this.blueTeam.add(player);
            if (this.arena.getTeamMeta().getBlueSpawnPoint() != null)
                player.teleport(this.arena.getTeamMeta().getBlueSpawnPoint());
            else
                player.teleport(this.arena.getBallSpawnLocation());
            if (this.getHologram() != null) {
                this.getHologram().show(player);
            }
            this.addPlayerToScoreboard(player);
            Bukkit.getPluginManager().callEvent(new GameJoinEvent(this, player));
            return true;
        }
        return false;
    }

    @Override
    public void reset() {
        this.bumpers.clear();
        super.reset(false);
        this.stage = GameStage.ENABLED;
        this.countdown = this.arena.getLobbyMeta().getCountDown();
        this.isStarting = false;
    }

    public void startGame() {
        this.countdown = this.arena.getLobbyMeta().getGameTime();
        this.stage = GameStage.RUNNING;
        for (final Player player : this.lobby) {
            if (this.preSelection.containsKey(player) &&
                    ((this.preSelection.get(player) == Team.RED && this.getRedTeamPlayers().length < this.arena.getTeamMeta().getTeamMaxSize()) ||
                            (this.preSelection.get(player) == Team.BLUE && this.getBlueTeamPlayers().length < this.arena.getTeamMeta().getTeamMaxSize())))
                this.join(player, this.preSelection.get(player));
            else {
                Team team = Team.RED;
                if (this.blueTeam.size() < this.redTeam.size()) {
                    team = Team.BLUE;
                }
                this.setTeam(player, team);
                this.join(player, team);
            }
        }
        this.lobby.clear();
        this.preSelection.clear();
    }

    void setTeam(Player player, Team team) {
        if (!this.lobby.contains(player))
            return;
        if (this.getAmountFromTeam(team) >= this.arena.getTeamMeta().getTeamMaxSize())
            return;
        if (team == Team.RED) {
            player.getInventory().setArmorContents(this.arena.getTeamMeta().getRedItems());
        } else {
            player.getInventory().setArmorContents(this.arena.getTeamMeta().getBlueItems());
        }
        player.setWalkSpeed(this.arena.getTeamMeta().getWalkingSpeed());
        this.preSelection.put(player, team);
    }

    private boolean endgame;

    public void endGame() {
        this.endgame = true;
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            HelperGameEntity.this.reset();
            HelperGameEntity.this.endgame = false;
        }, 20 * 5);
    }

    @Override
    public void run() {
        if (!this.arena.isEnabled())
            return;
        this.secondbumper--;
        if (this.secondbumper <= 0) {
            this.secondbumper = 20;
            this.updateSigns();
            this.fixCachedRangePlayers();
            if (this.stage == GameStage.RUNNING) {
                if (this.getPlayers().isEmpty()) {
                    this.reset();
                } else if (this.getPlayers().size() < this.arena.getLobbyMeta().getMinPlayers()) {
                    this.sendMessageToPlayers(Language.GAME_DRAW_TITLE, Language.GAME_DRAW_SUBTITLE);
                    this.endGame();
                }
                this.countdown--;
                for (final Player player : this.getPlayers()) {
                    player.setLevel(this.countdown);
                }
                if (this.gameScoreboard != null) {
                    this.gameScoreboard.update(this);
                }
                if (this.countdown <= 10 && !this.endgame) {
                    try {
                        this.blingsound.apply(this.getPlayers());
                    } catch (final Exception e) {
                        Bukkit.getServer().getConsoleSender().sendMessage(BlockBallPlugin.PREFIX_CONSOLE + ChatColor.RED + "Invalid 1.8/1.9 sound. [BlingSound]");
                    }
                }
                if (this.countdown == 0) {
                    NMSRegistry.addMoney(this.arena.getTeamMeta().getRewardGames(), this.blueTeam.toArray(new Player[this.blueTeam.size()]));
                    NMSRegistry.addMoney(this.arena.getTeamMeta().getRewardGames(), this.redTeam.toArray(new Player[this.redTeam.size()]));
                    this.executeCommand(this.arena.getTeamMeta().getGamendCommand(), this.getPlayers());
                    if (this.redGoals > this.blueGoals) {
                        this.executeCommand(this.arena.getTeamMeta().getWinCommand(), this.redTeam);
                        NMSRegistry.addMoney(this.arena.getTeamMeta().getRewardWinning(), this.redTeam.toArray(new Player[this.redTeam.size()]));
                        Bukkit.getPluginManager().callEvent(new GameWinEvent(this.redTeam, this));
                        this.sendMessageToPlayers(this.decryptText(this.arena.getTeamMeta().getRedwinnerTitleMessage()), this.decryptText(this.arena.getTeamMeta().getRedwinnerSubtitleMessage()));
                        this.endGame();
                    } else if (this.redGoals < this.blueGoals) {
                        this.executeCommand(this.arena.getTeamMeta().getWinCommand(), this.blueTeam);
                        NMSRegistry.addMoney(this.arena.getTeamMeta().getRewardWinning(), this.blueTeam.toArray(new Player[this.blueTeam.size()]));
                        Bukkit.getPluginManager().callEvent(new GameWinEvent(this.blueTeam, this));
                        this.sendMessageToPlayers(this.decryptText(this.arena.getTeamMeta().getBluewinnerTitleMessage()), this.decryptText(this.arena.getTeamMeta().getBluewinnerSubtitleMessage()));
                        this.endGame();
                    } else {
                        this.sendMessageToPlayers(Language.GAME_DRAW_TITLE, Language.GAME_DRAW_SUBTITLE);
                        this.endGame();
                    }
                }
            }
            if (this.stage == GameStage.ENABLED) {
                if (!this.lobby.isEmpty()
                        && this.lobby.size() >= this.arena.getLobbyMeta().getMinPlayers()
                        && (!this.arena.getTeamMeta().isForceEvenTeamsEnabled() || this.lobby.size() % 2 == 0)) {
                    if (!this.isStarting) {
                        this.isStarting = true;
                    } else {
                        this.countdown--;
                        for (final Player player : this.lobby) {
                            player.setLevel(this.countdown);
                        }
                        if (this.countdown <= 10) {
                            for (final Player player : this.lobby) {
                                final float exp = ((float) (10 - this.countdown)) / 10;
                                player.setExp(exp);
                            }
                        }
                        if (this.countdown <= 5) {
                            try {
                                this.blingsound.apply(this.lobby);
                            }catch (final Exception e) {
                                Bukkit.getServer().getConsoleSender().sendMessage(BlockBallPlugin.PREFIX_CONSOLE + ChatColor.RED + "Invalid 1.8/1.9 sound. [BlingSound]");
                            }
                        }
                        if (this.countdown <= 0) {
                            this.isStarting = false;
                            this.countdown = this.arena.getLobbyMeta().getCountDown();
                            if (this.getHologram() != null)
                                this.getHologram().setText(this.decryptText(this.arena.getTeamMeta().getHologramText()));
                            this.startGame();
                        }
                    }
                } else {
                    if (this.isStarting) {
                        this.countdown = this.arena.getLobbyMeta().getCountDown();
                        this.isStarting = false;
                        for (final Player player : this.lobby) {
                            player.setLevel(0);
                            player.setExp(0.0F);
                        }
                    }
                }
            }
        }
        this.playerForcefield();
        super.run();
    }

    boolean canJoinLobby(Player player) {
        return !this.isLobbyFull() && !this.lobby.contains(player) && this.stage == GameStage.ENABLED;
    }

    boolean isLobbyFull() {
        return (this.arena.getLobbyMeta().getMaxPlayers() * 2) <= this.lobby.size();
    }

    boolean isInLobby(Player player) {
        return this.lobby.contains(player);
    }

    private int getAmountFromTeam(Team teamOther) {
        int i = 0;
        for (final Team team : this.preSelection.values()) {
            if (teamOther == team)
                i++;
        }
        return i;
    }

    @Override
    protected String decryptText(String text) {
        try {
            text = ChatColor.translateAlternateColorCodes('&', text.replace(":countdown", String.valueOf(this.countdown)));
        } catch (final Exception e) {
            this.sendErrorMessage();
        }
        return super.decryptText(text);
    }

    @Override
    public GameStage getStage() {
        return this.stage;
    }

    private void updateSigns() {
        final IPosition[] signPositions = this.arena.getLobbyMeta().getSignPositions().toArray(new IPosition[this.arena.getLobbyMeta().getSignPositions().size()]);
        for (final IPosition position : signPositions) {
            final Location location = position.toLocation();
            if (location.getBlock().getType() == Material.SIGN_POST || location.getBlock().getType() == Material.WALL_SIGN) {
                ConfigOld.getInstance().getMinigameSign().updateJoinSignConsideringMaxPlayers((Sign) location.getBlock().getState(), this, this.lobby);
            } else {
                this.arena.getLobbyMeta().removeSignPosition(position);
            }
        }
        final IPosition[] redSignPositions = this.arena.getLobbyMeta().getRedTeamSignPositions().toArray(new IPosition[this.arena.getLobbyMeta().getRedTeamSignPositions().size()]);
        for (final IPosition position : redSignPositions) {
            final Location location = position.toLocation();
            if (location.getBlock().getType() == Material.SIGN_POST || location.getBlock().getType() == Material.WALL_SIGN) {
                ConfigOld.getInstance().getTeamSign().updateTeamSignConsideringMinigame((Sign) location.getBlock().getState(), this, Team.RED, this.preSelection);
            } else {
                this.arena.getLobbyMeta().removeRedTeamSignPosition(position);
            }
        }
        final IPosition[] blueSignPositions = this.arena.getLobbyMeta().getBlueTeamSignPositions().toArray(new IPosition[this.arena.getLobbyMeta().getBlueTeamSignPositions().size()]);
        for (final IPosition position : blueSignPositions) {
            final Location location = position.toLocation();
            if (location.getBlock().getType() == Material.SIGN_POST || location.getBlock().getType() == Material.WALL_SIGN) {
                ConfigOld.getInstance().getTeamSign().updateTeamSignConsideringMinigame((Sign) location.getBlock().getState(), this, Team.BLUE, this.preSelection);
            } else {
                this.arena.getLobbyMeta().removeBlueTeamSignPosition(position);
            }
        }
    }

    void storeTemporaryInventory(Player player) {
        final TemporaryPlayerStorage storage = new TemporaryPlayerStorage();
        storage.armorContent = player.getInventory().getArmorContents().clone();
        storage.isFlying = player.getAllowFlight();
        storage.inventory = player.getInventory().getContents().clone();
        storage.gameMode = player.getGameMode();
        storage.level = player.getLevel();
        storage.exp = player.getExp();
        storage.health = player.getHealth();
        storage.foodLevel = player.getFoodLevel();
        storage.scoreboard = player.getScoreboard();
        storage.walkingSpeed = player.getWalkSpeed();
        this.temporaryStorage.put(player, storage);
    }

    private void playerForcefield() {
        this.bumeper--;
        if (this.bumeper <= 0) {
            this.bumeper = 40;
            for (final Player player : this.getPlayers()) {
                if (!this.arena.isLocationInArea(player.getLocation()) && !SChatMenuManager.getInstance().isUsing(player)) {
                    if (!this.bumpers.containsKey(player))
                        this.bumpers.put(player, 0);
                    else
                        this.bumpers.put(player, this.bumpers.get(player) + 1);
                    final Vector knockback = this.arena.getBallSpawnLocation().toVector().subtract(player.getLocation().toVector());
                    player.getLocation().setDirection(knockback);
                    player.setVelocity(knockback);
                    final Vector direction = this.arena.getBallSpawnLocation().toVector().subtract(player.getLocation().toVector());
                    player.setVelocity(direction.multiply(0.1));
                    if (this.bumpers.get(player) == 5) {
                        player.teleport(this.arena.getBallSpawnLocation());
                    }
                } else if (this.bumpers.containsKey(player)) {
                    this.bumpers.remove(player);
                }
            }
        }
    }

    /**
     private final String[] hubGameSign = null;

    private final HubLobbyMeta hubLobbyMeta = null;

    public HubGame(Arena arena) {
      //  super(arena);
      //  this.hubLobbyMeta = arena.getMeta().find(HubLobbyMeta.class).get();
       // this.hubGameSign = Config.getInstance().getHubGameSign();
    }

    @Override
    public Optional<BlockBallPlayer> getBlockBallPlayerFromPlayer(Object player) {
        return null;
    }

    /**
     * Returns the status of a game.
     *
     * @return status

@Override
public GameStatus getStatus() {
        return null;
        }

/**
 * Adds a player to the game returns false if he doesn't meet the required options.
 *
 * @param player player - @NotNull
 * @param team   team - @Nullable, team gets automatically selection
 * @return success

@Override
public boolean join(Object player, Team team) {
        this.leave(player);
        Team joiningTeam = team;
        if (this.customizingMeta.isAutoSelectTeamEnabled()) {
        if (this.redTeamPlayers.size() <= this.blueTeamPlayers.size()) {
        joiningTeam = Team.RED;
        } else {
        joiningTeam = Team.BLUE;
        }
        }
        /*if (joiningTeam == Team.RED && this.redTeamPlayers.size() < this.redTeamMeta.getMaxAmountOfPlayers()) {
            final GameJoinEvent event = new GameJoinEvent(this, (Player) player);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.redTeamPlayers.add((Player) player);
                this.preparePlayerForGame((Player) player, team, this.redTeamMeta);
                return true;
            }
            return false;
        } else if (joiningTeam == Team.BLUE && this.blueTeamPlayers.size() < this.blueTeamMeta.getMaxAmountOfPlayers()) {
            final GameJoinEvent event = new GameJoinEvent(this, (Player) player);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.blueTeamPlayers.add((Player) player);
                this.preparePlayerForGame((Player) player, team, this.blueTeamMeta);
                return true;
            }
            return false;
        }
        return false;
        }

/**
 * Removes a player from the given game returns false if it did not work.
 *
 * @param player player - @NotNull
 * @return success

@Override
public boolean leave(Object player) {
     /*   final Optional<GamePlayer> optPlayer = this.getGamePlayerByPlayer((Player) player);
        if (optPlayer.isPresent()) {
            final GamePlayer gamePlayer = optPlayer.get();
            if (gamePlayer.team == Team.BLUE) {
                this.blueTeamPlayers.remove(player);
            } else {
                this.redTeamPlayers.remove(player);
            }
            this.gamePlayers.remove(gamePlayer);

            gamePlayer.restoreFromHubGame();

            ((Player) player).sendMessage(this.customizingMeta.getLeaveMessage().get());

            final GameLeaveEvent gameLeaveEvent = new GameLeaveEvent(this, (Player) player);
            Bukkit.getPluginManager().callEvent(gameLeaveEvent);
            return true;
        }
        return false;
        }

/**
 * Returns the team by the given player. Returns null if player is not in this game.
 *
 * @param player player
 * @return team

@Override
public Team getTeamFromPlayer(Object player) {
        return null;
        }

/**
 * Removes the ground item.
 *

@Override
public void removeGroundItem(Object item) {

        }

/**
 * Returns a value for the given place holder. Returns empty string if not found.
 *
 * @param type type
 * @return value

@Override
public String getValueForPlaceHolder(Object type) {
        return null;
        }

/**
 * Gets called when a team scores enough goals to reach the max amount of goals.
 *
 * @param teamMeta teamMeta of the winning team

@Override
public void onWin(TeamMeta teamMeta) {
       /* ScreenUtils.setTitle(this.replaceMessagePlaceholders(teamMeta.getWinningMessageTitle().get())
                , teamMeta.getWinningMessageSubtitle().get()
                , 0, 20 * 3, 10, (List<Player>) (Object) this.getPlayers());
        try {
            this.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        }

/**
 * Gets called when a team scores a goal.
 *
 * @param teamMeta teamMeta of the scorring team

@Override
public void onScore(TeamMeta teamMeta) {
        try {
        //   this.arena.getBallGoalParticle().apply(this.ball.getLocation());
        //  this.arena.getBallGoalSound().applyToLocation(this.ball.getLocation());
        } catch (final Exception e) {
        Bukkit.getServer().getConsoleSender().sendMessage(BlockBallPlugin.Companion.getPREFIX_CONSOLE() + ChatColor.RED + "Invalid 1.8/1.9 sound. [BallGoalSound]");
        }
     /*   ScreenUtils.setTitle(this.replaceMessagePlaceholders(teamMeta.getScoreMessageTitle().get())
                , teamMeta.getScoreMessageSubtitle().get()
                , 0, 20 * 3, 10, (List<Player>) (Object) this.getPlayers());
        }


@Override
public void onUpdateSigns() {
        /*for (int i = 0; i < this.hubLobbyMeta.getRedTeamSignPositions().size(); i++) {
            final IPosition position = this.hubLobbyMeta.getRedTeamSignPositions().get(i);
            final Location location = ((LocationBuilder) position).toLocation();
            if (location.getBlock().getType() == Material.SIGN_POST || location.getBlock().getType() == Material.WALL_SIGN) {
                this.replacePlaceTextOnSign((Sign) location.getBlock().getState()
                        , this.hubGameSign, this.redTeamMeta, this.redTeamPlayers.size(), this.redTeamMeta.getMaxAmountOfPlayers());
            } else {
                this.hubLobbyMeta.removeRedTeamSignPosition(position);
            }
        }
        for (int i = 0; i < this.hubLobbyMeta.getBlueTeamSignPositions().size(); i++) {
            final IPosition position = this.hubLobbyMeta.getBlueTeamSignPositions().get(i);
            final Location location = ((LocationBuilder) position).toLocation();
            if (location.getBlock().getType() == Material.SIGN_POST || location.getBlock().getType() == Material.WALL_SIGN) {
                this.replacePlaceTextOnSign((Sign) location.getBlock().getState()
                        , this.hubGameSign, this.blueTeamMeta, this.blueTeamPlayers.size(), this.blueTeamMeta.getMaxAmountOfPlayers());
            } else {
                this.hubLobbyMeta.removeBlueTeamSignPosition(position);
            }
        }
        }

private void preparePlayerForGame(Player player, Team team, TeamMeta teamMeta) {
      /*  final GamePlayer gamePlayer = new GamePlayer(player);
        this.gamePlayers.add(gamePlayer);
        gamePlayer.storeForHubGame();
        gamePlayer.team = team;*/

    /*    player.setWalkSpeed(teamMeta.getWalkingSpeed());
        player.getInventory().setArmorContents((ItemStack[]) teamMeta.getArmorContents());

        if (teamMeta.getSpawnPoint().isPresent()) {
            player.teleport((Location) teamMeta.getSpawnPoint().get());
        } else {
          //  player.teleport((Location) this.getArena().getBallSpawnLocation());
        }

        player.sendMessage(Config.getInstance().getPrefix() + teamMeta.getJoinMessage());
    }

    /**
     * Returns all positions of signs of the given team.
     *
     * @param team team
     * @return signs
     */
   /* @Override
    public IPosition getTeamSignPositions(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("Team cannot be null!");
        } else if (team == Team.BLUE) {
            return this.hubLobbyMeta.getBlueTeamSignPositions();
        } else {
            return this.hubLobbyMeta.getRedTeamSignPositions();
        }
    }*/


