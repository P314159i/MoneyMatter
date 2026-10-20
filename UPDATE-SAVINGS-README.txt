Pocket Ledger 1.1 — savings by source

This ZIP is a SOURCE UPDATE, not a compiled APK.

1. Export a backup in the installed app before updating.
2. Extract the app folder from this ZIP into your existing MoneyMatter project folder. Merge folders and replace matching files. Keep your existing local.properties, Gradle configuration and signing keys.
3. From MoneyMatter, use your existing build command:
   JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 "$HOME/tools/gradle-8.9/bin/gradle" assembleDebug
4. Copy app/build/outputs/apk/debug/app-debug.apk to your phone and install as an update. Do not uninstall the old app. The application ID is unchanged, and the existing computer's debug signing key must be retained.

New features:
- Accounts & backup: name each opening savings source and amount. Duplicate names group together.
- Savings by source: see each source's remaining balance across all dates.
- Add savings: record later receipts from a named source, separately from income to spending.
- Savings transfers, withdrawals and direct savings expenses: specify their source.
- Existing unlabelled opening savings and earlier transactions appear as Unassigned. Earlier transactions remain unlabelled; changing opening source rows does not relabel them.

Your stated savings can be recorded as two opening rows named Mom, for 4500 and 5500, provided those amounts were already included in your starting savings. These replace the existing Unassigned opening row; do not add them on top of the same existing balance. The app does not insert any personal amounts automatically.

The update reads existing records and earlier backups. Export/import includes source details. The older app does not understand new savings-deposit entries; retain the new app for new backups.

Verification: JavaScript syntax and automated savings accounting tests passed. Android compilation and device testing have not been run for this update. Java Activity code is unchanged.
