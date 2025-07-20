package me.ksyz.accountmanager;

import com.google.gson.*;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.utils.SSLUtil;
import me.ksyz.accountmanager.auth.AccountType;
import me.ksyz.accountmanager.auth.CookieAuth;
import me.ksyz.accountmanager.gui.GuiCookieAuth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

@Mod(modid = "accountmanager", version = "@VERSION@", clientSideOnly = true, acceptedMinecraftVersions = "1.8.9")
public class AccountManager {
  private static final Minecraft mc = Minecraft.getMinecraft();
  private static final File file = new File(mc.mcDataDir, "accounts.json");
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public static final ArrayList<Account> accounts = new ArrayList<>();

  @EventHandler
  public static void init(FMLInitializationEvent event) {
    SSLUtil.getSSLContext();
    MinecraftForge.EVENT_BUS.register(new Events());

    if (!file.exists()) {
      try {
        if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
          if (file.createNewFile()) {
            System.out.print("Successfully created accounts.json!");
          }
        }
      } catch (IOException e) {
        System.err.print("Couldn't create accounts.json!");
      }
    }
  }

  public static void load() {
    accounts.clear();
    try {
      JsonElement json = new JsonParser().parse(new BufferedReader(new FileReader(file)));
      if (json instanceof JsonArray) {
        JsonArray jsonArray = json.getAsJsonArray();
        for (JsonElement jsonElement : jsonArray) {
          JsonObject jsonObject = jsonElement.getAsJsonObject();
          accounts.add(Account.fromJson(jsonObject));
        }
      }
    } catch (FileNotFoundException e) {
      System.err.print("Couldn't find accounts.json!");
    } catch (JsonSyntaxException e) {
      System.err.println("Error parsing accounts.json: " + e.getMessage());
    }
  }

  public static void save() {
    try {
      JsonArray jsonArray = new JsonArray();
      for (Account account : accounts) {
        jsonArray.add(account.toJson());
      }
      PrintWriter printWriter = new PrintWriter(new FileWriter(file));
      printWriter.println(gson.toJson(jsonArray));
      printWriter.close();
    } catch (IOException e) {
      System.err.print("Couldn't save accounts.json!");
    }
  }

  /**
   * Adds a cracked account to the account manager.
   * Cracked accounts typically do not have a real UUID from Mojang/Microsoft,
   * so an empty string is provided for the UUID.
   *
   * @param username The username of the cracked account.
   */
  public static void addCrackedAccount(String username) {
    Optional<Account> existingAccount = accounts.stream()
            .filter(acc -> acc.getUsername().equalsIgnoreCase(username) && acc.getType() == AccountType.CRACKED)
            .findFirst();

    if (existingAccount.isPresent()) {
      System.out.println("Cracked account " + username + " already exists. Skipping add.");
      return;
    }
    accounts.add(new Account(
            "",
            "accessToken",
            username,
            "",
            0L,
            0L,
            AccountType.CRACKED
    ));
    save();
    System.out.println("Cracked account " + username + " added successfully!");
  }


  public static void addAccountFromCookieFile(File cookieFile, GuiScreen previousScreen) {
    GuiCookieAuth gui = new GuiCookieAuth(previousScreen);
    CookieAuth.addAccountFromCookieFile(cookieFile, gui);
  }
}