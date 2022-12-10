# Contributions / Beiträge an der Weiterentwicklung

Die Software ist Opensource und lebt von Ideen und Beiträgen, die von Interessierten beigesteuert werden können.

Folgende Beiträge sind explizit erwünscht:

## Melden eines Issues
Unter [Issues](https://github.com/luechtdiode/mobile-queue/issues) können beliebige Themen aufgegriffen und dokumentiert werden. Ein Issue kann ein nicht unterstützter Anwendungsfall, eine Fehlfunktion im Programm, eine Idee für eine ergonomische Verbesserung, ein Verbesserungsvorschlag für eine textuelle Formulierung usw. sein.

## Testen von Fehlerbehebungen, neuen Features
Wenn ein Issue umgesetzt wird, muss getestet werden, dass die Anpassungen den Erwartungen entsprechen und dass sich die Software dadurch weiterhin stabil betreiben lässt.
Es existieren bereits weinige automatisierte Tests. Diese können nicht das ganze Spektrum abdecken. Es ist also sehr Wertvoll, wenn diese Qualitätssicherung durch Test-Personen durchgeführt werden kann.

## Erstellen von Dokumentationen
Beiträge für die Verbesserung und Erweiterung der Dokumentation sind sehr erwünscht.

## Pflege und Weiterentwicklung des Programm-Codes
* Docu [How to setup local Dev Environment](docs/LocalDevSetup.md)
* Refactorings
* Automatisierung / Erweiterung der Testabdeckung
* Optimieren der Build-Pipeline
* Bearbeitung der Issues, welche zu Programm-Anpassungen führen.

## Submitting changes

Please send a GitHub Pull Request with a clear list of what you've done (read more about pull requests).
We can always use more test coverage. Please follow our coding conventions (below) and make sure all of
your commits are atomic (one feature per commit).

* Do not commit code containing secrets.
* Always write a clear log message for your commits.

* In the commit summary, describe the action/intention of the commit like
  * add new feature
  * fix nullpointer reading bla bla
  * clean code: optimize imports
  * refactoring: inline xy

* One-line messages are fine for small changes, but bigger changes should look like this:
```bash
$ git commit -m "A brief summary of the commit
> 
> A paragraph describing what changed and its impact."
```
* Rebase on origin/master branch before create a pull request.

## Coding conventions

Start reading our code and you'll get the hang of it. We optimize for readability:

* use default code-formatter of your IDE before each commit, only on changed files.
* try to remove compiler-warnings before each commit.
* use spell-checker for documentations.
* anonymize screenshots with sensitive content.
* use short names in small scopes and use semantic pregnant names in more global scopes.
* try to keep contezeptually consistent with your changes to the rest of the implementations.
* check for updated dependencies.
* this is open source software. Consider the people who will read your code, and make it look nice for them. It's sort of like driving a car: Perhaps you love doing donuts when you're alone, but with passengers the goal is to make the ride as smooth as possible.

[See more specific guidelines in the docs subfolder ...](docs)
