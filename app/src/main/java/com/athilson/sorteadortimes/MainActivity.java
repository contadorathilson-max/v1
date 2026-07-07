package com.athilson.sorteadortimes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "sorteador_times_prefs";
    private static final String PLAYERS_KEY = "players_json";

    private final ArrayList<Player> players = new ArrayList<>();
    private final ArrayList<Button> addStarButtons = new ArrayList<>();

    private EditText nameInput;
    private EditText teamsInput;
    private EditText playersPerTeamInput;
    private LinearLayout playersContainer;
    private LinearLayout teamsContainer;
    private LinearLayout rankingContainer;
    private TextView summaryText;
    private TextView selectedStarsText;

    private int selectedStars = 3;
    private String lastDrawText = "";

    private final int greenDark = Color.rgb(10, 55, 24);
    private final int green = Color.rgb(27, 94, 32);
    private final int greenLight = Color.rgb(232, 245, 233);
    private final int red = Color.rgb(170, 30, 30);
    private final int grayText = Color.rgb(65, 65, 65);
    private final int grayBorder = Color.rgb(215, 225, 215);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPlayers();
        buildScreen();
        refreshAll();
    }

    private void buildScreen() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.rgb(248, 250, 248));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Sorteador de Times");
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(greenDark);
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidth());

        TextView subtitle = new TextView(this);
        subtitle.setText("Equilibre os times por nível, registre gols e assistências");
        subtitle.setTextSize(14);
        subtitle.setTextColor(grayText);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(4), 0, dp(4));
        root.addView(subtitle, fullWidth());

        TextView developer = new TextView(this);
        developer.setText("Desenvolvido por Athilson Alves");
        developer.setTextSize(13);
        developer.setTextColor(grayText);
        developer.setGravity(Gravity.CENTER);
        developer.setPadding(0, 0, 0, dp(16));
        root.addView(developer, fullWidth());

        summaryText = new TextView(this);
        summaryText.setTextSize(15);
        summaryText.setTypeface(Typeface.DEFAULT_BOLD);
        summaryText.setTextColor(Color.WHITE);
        summaryText.setPadding(dp(12), dp(10), dp(12), dp(10));
        summaryText.setGravity(Gravity.CENTER);
        summaryText.setBackground(cardBackground(green, dp(12), greenDark));
        root.addView(summaryText, fullWidthWithBottomMargin(dp(14)));

        LinearLayout addCard = sectionCard();
        addCard.addView(sectionTitle("Cadastrar jogador"));

        addCard.addView(label("Nome do jogador"));
        nameInput = new EditText(this);
        nameInput.setHint("Ex.: João, Pedro, Lucas...");
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        addCard.addView(nameInput, fullWidthWithBottomMargin(dp(10)));

        selectedStarsText = label("Nível selecionado: 3 estrelas");
        selectedStarsText.setTypeface(Typeface.DEFAULT_BOLD);
        addCard.addView(selectedStarsText, fullWidthWithBottomMargin(dp(6)));
        addCard.addView(createAddStarSelector(), fullWidthWithBottomMargin(dp(12)));

        Button addButton = primaryButton("Adicionar jogador");
        addButton.setOnClickListener(v -> addPlayer());
        addCard.addView(addButton, fullWidth());
        root.addView(addCard, fullWidthWithBottomMargin(dp(14)));

        LinearLayout drawCard = sectionCard();
        drawCard.addView(sectionTitle("Configurar sorteio"));

        LinearLayout drawInputs = new LinearLayout(this);
        drawInputs.setOrientation(LinearLayout.HORIZONTAL);

        teamsInput = numberInput("2");
        playersPerTeamInput = numberInput("5");
        drawInputs.addView(inputBlock("Quantidade de times", teamsInput), weightWithEndMargin(1, dp(8)));
        drawInputs.addView(inputBlock("Jogadores por time", playersPerTeamInput), weight(1));
        drawCard.addView(drawInputs, fullWidthWithBottomMargin(dp(12)));

        Button drawButton = primaryButton("Sortear times equilibrados");
        drawButton.setOnClickListener(v -> drawTeams());
        drawCard.addView(drawButton, fullWidthWithBottomMargin(dp(8)));

        Button copyButton = secondaryButton("Copiar último sorteio");
        copyButton.setOnClickListener(v -> copyLastDraw());
        drawCard.addView(copyButton, fullWidthWithBottomMargin(dp(8)));

        LinearLayout drawActions = new LinearLayout(this);
        drawActions.setOrientation(LinearLayout.HORIZONTAL);
        Button clearStatsButton = secondaryButton("Zerar estatísticas");
        clearStatsButton.setOnClickListener(v -> confirmResetStats());
        drawActions.addView(clearStatsButton, weightWithEndMargin(1, dp(8)));

        Button clearPlayersButton = dangerButton("Limpar lista");
        clearPlayersButton.setOnClickListener(v -> confirmClearPlayers());
        drawActions.addView(clearPlayersButton, weight(1));
        drawCard.addView(drawActions, fullWidth());
        root.addView(drawCard, fullWidthWithBottomMargin(dp(14)));

        LinearLayout listCard = sectionCard();
        listCard.addView(sectionTitle("Jogadores cadastrados"));
        playersContainer = new LinearLayout(this);
        playersContainer.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(playersContainer, fullWidth());
        root.addView(listCard, fullWidthWithBottomMargin(dp(14)));

        LinearLayout rankingCard = sectionCard();
        rankingCard.addView(sectionTitle("Ranking de gols e assistências"));
        rankingContainer = new LinearLayout(this);
        rankingContainer.setOrientation(LinearLayout.VERTICAL);
        rankingCard.addView(rankingContainer, fullWidth());
        root.addView(rankingCard, fullWidthWithBottomMargin(dp(14)));

        LinearLayout resultCard = sectionCard();
        resultCard.addView(sectionTitle("Resultado do sorteio"));
        teamsContainer = new LinearLayout(this);
        teamsContainer.setOrientation(LinearLayout.VERTICAL);
        resultCard.addView(teamsContainer, fullWidth());
        root.addView(resultCard, fullWidthWithBottomMargin(dp(14)));

        TextView footer = new TextView(this);
        footer.setText("Dica: use 5 estrelas para os jogadores mais fortes e 1 estrela para os iniciantes. Depois do jogo, registre gols e assistências para acompanhar o desempenho.");
        footer.setTextSize(13);
        footer.setTextColor(grayText);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, dp(24));
        root.addView(footer, fullWidth());

        updateAddStarButtons();
        setContentView(scrollView);
    }

    private LinearLayout createAddStarSelector() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        addStarButtons.clear();

        for (int i = 1; i <= 5; i++) {
            final int level = i;
            Button button = smallButton(i + "★");
            button.setOnClickListener(v -> {
                selectedStars = level;
                updateAddStarButtons();
            });
            addStarButtons.add(button);
            row.addView(button, weightWithEndMargin(1, i < 5 ? dp(6) : 0));
        }
        return row;
    }

    private void updateAddStarButtons() {
        if (selectedStarsText != null) {
            selectedStarsText.setText("Nível selecionado: " + selectedStars + " " + (selectedStars == 1 ? "estrela" : "estrelas") + "  " + stars(selectedStars));
        }
        for (int i = 0; i < addStarButtons.size(); i++) {
            Button button = addStarButtons.get(i);
            int level = i + 1;
            if (level == selectedStars) {
                button.setTextColor(Color.WHITE);
                button.setBackground(cardBackground(green, dp(10), greenDark));
            } else {
                button.setTextColor(greenDark);
                button.setBackground(cardBackground(Color.WHITE, dp(10), Color.rgb(160, 190, 160)));
            }
        }
    }

    private void addPlayer() {
        String name = nameInput.getText().toString().trim();

        if (name.isEmpty()) {
            toast("Digite o nome do jogador.");
            return;
        }

        int addedStars = selectedStars;
        players.add(new Player(name, addedStars, 0, 0));
        savePlayers();
        nameInput.setText("");
        selectedStars = 3;
        updateAddStarButtons();
        teamsContainer.removeAllViews();
        lastDrawText = "";
        refreshAll();
        toast("Jogador adicionado com " + addedStars + " " + (addedStars == 1 ? "estrela." : "estrelas."));
    }

    private void refreshAll() {
        refreshPlayers();
        refreshRanking();
        refreshSummary();
    }

    private void refreshPlayers() {
        playersContainer.removeAllViews();
        if (players.isEmpty()) {
            playersContainer.addView(infoText("Nenhum jogador cadastrado ainda."));
            return;
        }

        for (Player player : players) {
            LinearLayout card = miniCard();

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView name = new TextView(this);
            name.setText(player.name);
            name.setTextSize(18);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setTextColor(greenDark);
            header.addView(name, weight(1));

            Button renameButton = smallButton("Editar");
            renameButton.setOnClickListener(v -> renamePlayer(player));
            header.addView(renameButton, wrapWithEndMargin(dp(6)));

            Button deleteButton = dangerButton("Remover");
            deleteButton.setOnClickListener(v -> confirmDeletePlayer(player));
            header.addView(deleteButton, wrap());
            card.addView(header, fullWidthWithBottomMargin(dp(8)));

            TextView level = new TextView(this);
            level.setText("Nível: " + stars(player.stars) + "  (" + player.stars + "/5)");
            level.setTextSize(15);
            level.setTypeface(Typeface.DEFAULT_BOLD);
            level.setTextColor(greenDark);
            card.addView(level, fullWidthWithBottomMargin(dp(6)));

            card.addView(createPlayerStarSelector(player), fullWidthWithBottomMargin(dp(10)));

            TextView stats = new TextView(this);
            stats.setText(String.format(Locale.getDefault(), "Gols: %d   |   Assistências: %d", player.goals, player.assists));
            stats.setTextSize(15);
            stats.setTextColor(grayText);
            card.addView(stats, fullWidthWithBottomMargin(dp(8)));

            LinearLayout goalRow = new LinearLayout(this);
            goalRow.setOrientation(LinearLayout.HORIZONTAL);
            Button removeGoalButton = secondaryButton("- Gol");
            removeGoalButton.setOnClickListener(v -> {
                if (player.goals > 0) player.goals--;
                savePlayers();
                refreshAll();
            });
            goalRow.addView(removeGoalButton, weightWithEndMargin(1, dp(8)));

            Button goalButton = primaryButton("+ Gol");
            goalButton.setOnClickListener(v -> {
                player.goals++;
                savePlayers();
                refreshAll();
            });
            goalRow.addView(goalButton, weight(1));
            card.addView(goalRow, fullWidthWithBottomMargin(dp(8)));

            LinearLayout assistRow = new LinearLayout(this);
            assistRow.setOrientation(LinearLayout.HORIZONTAL);
            Button removeAssistButton = secondaryButton("- Assistência");
            removeAssistButton.setOnClickListener(v -> {
                if (player.assists > 0) player.assists--;
                savePlayers();
                refreshAll();
            });
            assistRow.addView(removeAssistButton, weightWithEndMargin(1, dp(8)));

            Button assistButton = primaryButton("+ Assistência");
            assistButton.setOnClickListener(v -> {
                player.assists++;
                savePlayers();
                refreshAll();
            });
            assistRow.addView(assistButton, weight(1));
            card.addView(assistRow, fullWidth());

            playersContainer.addView(card, fullWidthWithBottomMargin(dp(10)));
        }
    }

    private LinearLayout createPlayerStarSelector(Player player) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        for (int i = 1; i <= 5; i++) {
            final int level = i;
            Button button = smallButton(i + "★");
            if (level == player.stars) {
                button.setTextColor(Color.WHITE);
                button.setBackground(cardBackground(green, dp(10), greenDark));
            }
            button.setOnClickListener(v -> {
                player.stars = level;
                savePlayers();
                teamsContainer.removeAllViews();
                lastDrawText = "";
                refreshAll();
                toast(player.name + " agora está com " + level + " estrelas.");
            });
            row.addView(button, weightWithEndMargin(1, i < 5 ? dp(6) : 0));
        }
        return row;
    }

    private void renamePlayer(Player player) {
        final EditText input = new EditText(this);
        input.setText(player.name);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        new AlertDialog.Builder(this)
                .setTitle("Editar jogador")
                .setMessage("Altere o nome do jogador.")
                .setView(input)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        player.name = newName;
                        savePlayers();
                        teamsContainer.removeAllViews();
                        lastDrawText = "";
                        refreshAll();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmDeletePlayer(Player player) {
        new AlertDialog.Builder(this)
                .setTitle("Remover jogador")
                .setMessage("Deseja remover " + player.name + " da lista?")
                .setPositiveButton("Remover", (dialog, which) -> {
                    players.remove(player);
                    savePlayers();
                    teamsContainer.removeAllViews();
                    lastDrawText = "";
                    refreshAll();
                    toast("Jogador removido.");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void drawTeams() {
        teamsContainer.removeAllViews();
        lastDrawText = "";

        int teamsCount = parseInt(teamsInput, 2);
        int playersPerTeam = parseInt(playersPerTeamInput, 5);

        if (teamsCount < 2) {
            toast("Informe pelo menos 2 times.");
            return;
        }
        if (playersPerTeam < 1) {
            toast("Informe pelo menos 1 jogador por time.");
            return;
        }
        if (players.isEmpty()) {
            toast("Cadastre jogadores antes de sortear.");
            return;
        }

        int desiredSlots = teamsCount * playersPerTeam;
        ArrayList<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        ArrayList<Player> selectedPlayers = new ArrayList<>();
        ArrayList<Player> reservePlayers = new ArrayList<>();
        if (shuffledPlayers.size() > desiredSlots) {
            selectedPlayers.addAll(shuffledPlayers.subList(0, desiredSlots));
            reservePlayers.addAll(shuffledPlayers.subList(desiredSlots, shuffledPlayers.size()));
            toast("Há mais jogadores que vagas. Alguns ficarão como reserva.");
        } else {
            selectedPlayers.addAll(shuffledPlayers);
            if (shuffledPlayers.size() < desiredSlots) {
                toast("Faltam jogadores para completar todos os times.");
            }
        }

        Collections.sort(selectedPlayers, (p1, p2) -> {
            int starCompare = p2.stars - p1.stars;
            if (starCompare != 0) return starCompare;
            return p1.name.compareToIgnoreCase(p2.name);
        });

        ArrayList<ArrayList<Player>> teams = new ArrayList<>();
        int[] score = new int[teamsCount];
        for (int i = 0; i < teamsCount; i++) {
            teams.add(new ArrayList<>());
            score[i] = 0;
        }

        for (Player player : selectedPlayers) {
            int bestTeamIndex = findBestTeam(teams, score, playersPerTeam);
            if (bestTeamIndex >= 0) {
                teams.get(bestTeamIndex).add(player);
                score[bestTeamIndex] += player.stars;
            }
        }

        showTeams(teams, score, reservePlayers);
    }

    private int findBestTeam(ArrayList<ArrayList<Player>> teams, int[] score, int playersPerTeam) {
        int bestIndex = -1;
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).size() >= playersPerTeam) continue;

            if (bestIndex == -1) {
                bestIndex = i;
                continue;
            }

            boolean lowerScore = score[i] < score[bestIndex];
            boolean sameScoreLessPlayers = score[i] == score[bestIndex] && teams.get(i).size() < teams.get(bestIndex).size();
            if (lowerScore || sameScoreLessPlayers) bestIndex = i;
        }
        return bestIndex;
    }

    private void showTeams(ArrayList<ArrayList<Player>> teams, int[] score, ArrayList<Player> reservePlayers) {
        teamsContainer.removeAllViews();
        DecimalFormat df = new DecimalFormat("0.0");
        StringBuilder copyText = new StringBuilder();
        copyText.append("Sorteio de times\n\n");

        for (int i = 0; i < teams.size(); i++) {
            ArrayList<Player> team = teams.get(i);
            Collections.sort(team, Comparator.comparing(p -> p.name.toLowerCase(Locale.getDefault())));

            LinearLayout card = miniCard();
            double average = team.isEmpty() ? 0 : (double) score[i] / team.size();

            TextView teamTitle = new TextView(this);
            teamTitle.setText(String.format(Locale.getDefault(), "Time %d", i + 1));
            teamTitle.setTextSize(19);
            teamTitle.setTypeface(Typeface.DEFAULT_BOLD);
            teamTitle.setTextColor(greenDark);
            card.addView(teamTitle, fullWidthWithBottomMargin(dp(4)));

            TextView teamInfo = new TextView(this);
            teamInfo.setText(String.format(Locale.getDefault(), "%d jogador(es) | Força total: %d | Média: %s", team.size(), score[i], df.format(average)));
            teamInfo.setTextSize(14);
            teamInfo.setTextColor(grayText);
            card.addView(teamInfo, fullWidthWithBottomMargin(dp(8)));

            copyText.append("Time ").append(i + 1).append(" - Força: ").append(score[i]).append("\n");

            if (team.isEmpty()) {
                card.addView(infoText("Sem jogadores."));
                copyText.append("Sem jogadores.\n");
            } else {
                for (Player player : team) {
                    TextView line = new TextView(this);
                    line.setText(String.format(Locale.getDefault(), "• %s  %s  | G: %d  A: %d", player.name, stars(player.stars), player.goals, player.assists));
                    line.setTextSize(15);
                    line.setTextColor(Color.rgb(30, 30, 30));
                    line.setPadding(0, dp(3), 0, dp(3));
                    card.addView(line, fullWidth());
                    copyText.append("- ").append(player.name).append(" ").append(stars(player.stars)).append("\n");
                }
            }
            copyText.append("\n");
            teamsContainer.addView(card, fullWidthWithBottomMargin(dp(10)));
        }

        if (!reservePlayers.isEmpty()) {
            Collections.sort(reservePlayers, Comparator.comparing(p -> p.name.toLowerCase(Locale.getDefault())));
            LinearLayout reserveCard = miniCard();
            TextView reserveTitle = new TextView(this);
            reserveTitle.setText("Reservas / ficaram de fora");
            reserveTitle.setTextSize(18);
            reserveTitle.setTypeface(Typeface.DEFAULT_BOLD);
            reserveTitle.setTextColor(red);
            reserveCard.addView(reserveTitle, fullWidthWithBottomMargin(dp(6)));
            copyText.append("Reservas / ficaram de fora\n");

            for (Player player : reservePlayers) {
                TextView line = new TextView(this);
                line.setText("• " + player.name + "  " + stars(player.stars));
                line.setTextSize(15);
                line.setTextColor(Color.rgb(30, 30, 30));
                line.setPadding(0, dp(2), 0, dp(2));
                reserveCard.addView(line, fullWidth());
                copyText.append("- ").append(player.name).append(" ").append(stars(player.stars)).append("\n");
            }
            teamsContainer.addView(reserveCard, fullWidthWithBottomMargin(dp(10)));
        }

        lastDrawText = copyText.toString().trim();
    }

    private void refreshRanking() {
        rankingContainer.removeAllViews();
        if (players.isEmpty()) {
            rankingContainer.addView(infoText("Cadastre jogadores para ver o ranking."));
            return;
        }

        ArrayList<Player> ranked = new ArrayList<>(players);
        Collections.sort(ranked, (p1, p2) -> {
            int goalsCompare = p2.goals - p1.goals;
            if (goalsCompare != 0) return goalsCompare;
            int assistsCompare = p2.assists - p1.assists;
            if (assistsCompare != 0) return assistsCompare;
            return p1.name.compareToIgnoreCase(p2.name);
        });

        int limit = Math.min(5, ranked.size());
        for (int i = 0; i < limit; i++) {
            Player player = ranked.get(i);
            TextView line = new TextView(this);
            line.setText(String.format(Locale.getDefault(), "%dº  %s — Gols: %d | Assistências: %d", i + 1, player.name, player.goals, player.assists));
            line.setTextSize(15);
            line.setTextColor(Color.rgb(30, 30, 30));
            line.setPadding(0, dp(4), 0, dp(4));
            rankingContainer.addView(line, fullWidth());
        }
    }

    private void copyLastDraw() {
        if (lastDrawText == null || lastDrawText.trim().isEmpty()) {
            toast("Faça um sorteio primeiro.");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Sorteio de times", lastDrawText);
        clipboard.setPrimaryClip(clip);
        toast("Sorteio copiado.");
    }

    private void confirmResetStats() {
        if (players.isEmpty()) {
            toast("Não há jogadores cadastrados.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Zerar estatísticas")
                .setMessage("Deseja zerar gols e assistências de todos os jogadores?")
                .setPositiveButton("Zerar", (dialog, which) -> resetStats())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void resetStats() {
        for (Player player : players) {
            player.goals = 0;
            player.assists = 0;
        }
        savePlayers();
        teamsContainer.removeAllViews();
        lastDrawText = "";
        refreshAll();
        toast("Estatísticas zeradas.");
    }

    private void confirmClearPlayers() {
        if (players.isEmpty()) {
            toast("A lista já está vazia.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Limpar lista")
                .setMessage("Deseja apagar todos os jogadores cadastrados? Essa ação não pode ser desfeita.")
                .setPositiveButton("Apagar tudo", (dialog, which) -> {
                    players.clear();
                    savePlayers();
                    teamsContainer.removeAllViews();
                    lastDrawText = "";
                    refreshAll();
                    toast("Lista apagada.");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void refreshSummary() {
        int totalGoals = 0;
        int totalAssists = 0;
        int totalStars = 0;
        for (Player player : players) {
            totalGoals += player.goals;
            totalAssists += player.assists;
            totalStars += player.stars;
        }
        double averageStars = players.isEmpty() ? 0 : (double) totalStars / players.size();
        summaryText.setText(String.format(Locale.getDefault(),
                "Jogadores: %d | Média: %.1f★ | Gols: %d | Assistências: %d",
                players.size(), averageStars, totalGoals, totalAssists));
    }

    private void savePlayers() {
        try {
            JSONArray array = new JSONArray();
            for (Player player : players) {
                JSONObject object = new JSONObject();
                object.put("name", player.name);
                object.put("stars", player.stars);
                object.put("goals", player.goals);
                object.put("assists", player.assists);
                array.put(object);
            }
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putString(PLAYERS_KEY, array.toString()).apply();
        } catch (Exception e) {
            toast("Não foi possível salvar os jogadores.");
        }
    }

    private void loadPlayers() {
        players.clear();
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String json = prefs.getString(PLAYERS_KEY, "[]");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                players.add(new Player(
                        object.optString("name", "Jogador"),
                        object.optInt("stars", 3),
                        object.optInt("goals", 0),
                        object.optInt("assists", 0)
                ));
            }
        } catch (Exception ignored) {
            players.clear();
        }
    }

    private String stars(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            builder.append(i <= count ? "★" : "☆");
        }
        return builder.toString();
    }

    private int parseInt(EditText input, int fallback) {
        try {
            String value = input.getText().toString().trim();
            if (value.isEmpty()) return fallback;
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private EditText numberInput(String defaultValue) {
        EditText editText = new EditText(this);
        editText.setText(defaultValue);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        editText.setGravity(Gravity.CENTER);
        editText.setTextSize(18);
        return editText;
    }

    private LinearLayout inputBlock(String title, EditText input) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        TextView label = label(title);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER);
        block.addView(label, fullWidthWithBottomMargin(dp(4)));
        block.addView(input, fullWidth());
        return block;
    }

    private TextView sectionTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(19);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(greenDark);
        view.setPadding(0, 0, 0, dp(10));
        return view;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(grayText);
        return view;
    }

    private TextView infoText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(grayText);
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private LinearLayout sectionCard() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(14), dp(14), dp(14), dp(14));
        layout.setBackground(cardBackground(Color.WHITE, dp(14), grayBorder));
        return layout;
    }

    private LinearLayout miniCard() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(12), dp(12), dp(12), dp(12));
        layout.setBackground(cardBackground(greenLight, dp(12), Color.rgb(190, 215, 190)));
        return layout;
    }

    private GradientDrawable cardBackground(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(cardBackground(green, dp(10), greenDark));
        button.setMinHeight(dp(42));
        button.setMinimumHeight(dp(42));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(greenDark);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(cardBackground(Color.WHITE, dp(10), Color.rgb(150, 190, 150)));
        button.setMinHeight(dp(42));
        button.setMinimumHeight(dp(42));
        return button;
    }

    private Button smallButton(String text) {
        Button button = secondaryButton(text);
        button.setTextSize(12);
        button.setMinHeight(dp(36));
        button.setMinimumHeight(dp(36));
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private Button dangerButton(String text) {
        Button button = smallButton(text);
        button.setTextColor(red);
        button.setBackground(cardBackground(Color.WHITE, dp(10), Color.rgb(230, 170, 170)));
        return button;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams fullWidthWithBottomMargin(int bottom) {
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, 0, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams weight(float weight) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight);
    }

    private LinearLayout.LayoutParams weightWithEndMargin(float weight, int end) {
        LinearLayout.LayoutParams params = weight(weight);
        params.setMargins(0, 0, end, 0);
        return params;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWithEndMargin(int end) {
        LinearLayout.LayoutParams params = wrap();
        params.setMargins(0, 0, end, 0);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static class Player {
        String name;
        int stars;
        int goals;
        int assists;

        Player(String name, int stars, int goals, int assists) {
            this.name = name;
            this.stars = Math.max(1, Math.min(5, stars));
            this.goals = Math.max(0, goals);
            this.assists = Math.max(0, assists);
        }
    }
}
