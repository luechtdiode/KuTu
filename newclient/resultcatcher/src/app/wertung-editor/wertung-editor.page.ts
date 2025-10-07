import { Component, NgZone, inject, viewChild } from '@angular/core';
import { WertungContainer, Wertung, ScoreCalcVariable, ScoreCalcVariables, AthletMediaIsAtStart, AthletMediaIsFree, AthletMediaIsPaused, AthletMediaIsRunning } from '../backend-types';
import { BehaviorSubject, Subject, Subscription, defer, of } from 'rxjs';
import { NavController, Platform, ToastController, AlertController, IonItemSliding } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { NgForm } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Keyboard } from '@capacitor/keyboard';
import { Capacitor } from '@capacitor/core';
import { turn10ProgrammNames } from '../utils';
import { debounceTime, distinctUntilChanged, map, filter, switchMap, tap, share } from 'rxjs/operators';
import { encodeURIComponent2 } from '../services/websocket.service';

@Component({
    selector: 'app-wertung-editor',
    templateUrl: './wertung-editor.page.html',
    styleUrls: ['./wertung-editor.page.scss'],
    standalone: false
  })
export class WertungEditorPage {
  navCtrl = inject(NavController);
  private route = inject(ActivatedRoute);
  private alertCtrl = inject(AlertController);
  toastController = inject(ToastController);
  backendService = inject(BackendService);
  platform = inject(Platform);
  private zone = inject(NgZone);

  constructor() {
    const backendService = this.backendService;

    // If we navigated to this page, we will have an item available as a nav param
    this.durchgang = backendService.durchgang;
    this.step = backendService.step;
    this.geraetId = backendService.geraet;
    // tslint:disable-next-line:radix
    const itemId = parseInt(this.route.snapshot.paramMap.get('itemId'));
    this.updateUI(backendService.wertungen.find(w => w.id === itemId));
    this.installLazyAction();
    this.backendService.durchgangStarted.pipe(
      map(dgl =>
      dgl.filter(dg =>
        encodeURIComponent2(dg.durchgang) === encodeURIComponent2(this.durchgang)
        && dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    )).subscribe(dg => {
      this.durchgangopen = dg;
      this.mediaButtonDisabled = !this.backendService.isWebsocketConnected() || !dg
    });
  }
  private itemOriginal: WertungContainer;
  private durchgangopen = false;
  public readonly form = viewChild<any>('wertungsform');
  public readonly enote = viewChild<{
    setFocus: () => void;
}>('enote');
  public readonly dnote = viewChild<{
    setFocus: () => void;
}>('dnote');

  private subscription: Subscription;
  private mediasubscription: Subscription;

  private readonly wertungChanged = new Subject<Wertung>();

  item: WertungContainer;
  _wertung: Wertung;
  lastValidatedWertung: Wertung;
  nextItem: WertungContainer
  exercices: ScoreCalcVariable[][];
  currentMediaState = 'unbekannt';
  currentMediaButtonText = 'Musik laden';
  currentMediaButtonIcon = 'play-circle-outline';
  currentMediaButtonDisabled = true;
  currentMediaReleaseButtonDisabled = true;
  currentMediaAction = () => {};

  geraetId: number;

  step: number;

  durchgang: string;

  waiting = false;

  isDNoteUsed = true;

  get wertung(): Wertung {
    return this._wertung;
  }
  set wertung(value: Wertung) {
    this._wertung = value;
    this.updateExercices();
  }
  mediaButtonDisabled = true;
  durchgangstate() {
    const connected = this.backendService.isWebsocketConnected() ? (this.durchgangopen ? 'gestartet' : 'gesperrt') : 'offline';
    return connected;
  }
  get connected(): boolean {
    return this.backendService.isWebsocketConnected();
  }

  groupBy = <T, K extends keyof any>(arr: T[], key: (i: T) => K) => arr.reduce(
    (groups, item) => {
      (groups[key(item)] ||= []).push(item);
      return groups;
    },
    {} as Record<K, T[]>
  );
  flatten<T>(arr: T[][]): T[] {
    return ([] as T[]).concat(...arr);
  }

  updateVariable(event, variable: ScoreCalcVariable) {
    const exercices = this.flatten(this.exercices);
    if (this.wertung.variables) {
        exercices
          .filter(e => e.prefix === 'A' || e.prefix === 'D')
          .forEach(v => {
            if (v.prefix === variable.prefix && v.name == variable.name && v.index === variable.index) {
              v.value = event || 0;
            }
          });
        exercices
          .filter(e => e.prefix === 'B' || e.prefix === 'E')
          .forEach(v => {
            if (v.prefix === variable.prefix && v.name == variable.name && v.index === variable.index) {
              v.value = event || 0;
            }
          });
        exercices
          .filter(e => e.prefix === 'P')
          .forEach(v => {
            if (v.prefix === variable.prefix && v.name == variable.name && v.index === variable.index) {
              v.value = event || 0;
            }
          });
      }
  }

  get dNoteLabel() {
    return this.item.isDNoteUsed && turn10ProgrammNames.indexOf(this.item.programm) > -1 ? "A-Note" : "D-Note";
  }

  get eNoteLabel() {
    return this.item.isDNoteUsed && turn10ProgrammNames.indexOf(this.item.programm) > -1 ? "B-Note" : "E-Note";
  }

  get dNote() {
    return this.wertung.noteD;
  }
  set dNote(value: number) {
    this.wertung.noteD = value;
    this.wertungChanged.next(this.wertung);
  }

  get eNote() {
    return this.wertung.noteE;
  }
  set eNote(value: number) {
    this.wertung.noteE = value;
    this.wertungChanged.next(this.wertung);
  }

  installLazyAction() {
    this.wertungChanged.pipe(
      filter(wertung => {
        if (this.wertung.variables) {
          return true;
        } else {
          const deOK = (wertung.noteD || 0) == 0 && (wertung.noteE || 0) == 0;
          const eOK = (wertung.noteE || 0) > 0;
          return deOK || eOK;
        }
      }),
      tap(wertung => {
        this.zone.run(() => {
          if (this.wertung.variables) {
            this.wertung.noteD = null;
            this.wertung.noteE = null;
          }
          this.wertung.endnote = null;
        })}),
      debounceTime(1500),
      share()
    ).subscribe(wertung => {
      this.zone.run(() => {
        this.calculateEndnote(wertung);
      });
    });
  }

  calculateEndnote(wertung: Wertung) {
    let toValidate: Wertung = Object.assign({}, wertung, {
      noteD: wertung.variables ? null : wertung.noteD,
      noteE: wertung.variables ? null : wertung.noteE,
      variables: wertung.variables,
      mediafile: wertung.mediafile
    });
    if (toValidate.variables || toValidate.noteD !== this.lastValidatedWertung?.noteD || toValidate.noteE !== this.lastValidatedWertung?.noteE) {
      if (this.wertung.variables) {
        this.wertung.noteD = null;
        this.wertung.noteE = null;
      }
      this.wertung.endnote = null;
      this.backendService.validateWertung(toValidate).subscribe({
        next: (w) => {
          this.zone.run(() => {
            this.lastValidatedWertung = toValidate;
            this.wertung.noteD = w.noteD;
            this.wertung.noteE = w.noteE;
            this.wertung.endnote = w.endnote;
          });
        },
        error: (err) => {
          console.log(err);
        }
      });
    }
  }

  ionViewWillLeave() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = undefined;
    }
    if (this.mediasubscription) {
      this.mediasubscription.unsubscribe();
      this.mediasubscription = undefined;
    }
  }

  ionViewWillEnter() {
    if (this.subscription) {
      this.subscription.unsubscribe();
      this.subscription = undefined;
    }
    if (this.mediasubscription) {
      this.mediasubscription.unsubscribe();
      this.mediasubscription = undefined;
    }
    this.mediasubscription = this.backendService.mediaStateChanged.subscribe(ms => {
      this.zone.run(() => {
        this.mediaStateChanged(ms);
      });
    });
    this.subscription = this.backendService.wertungUpdated.subscribe(wc => {
      this.zone.run(() => {
        if (wc.wertung.athletId === this.wertung.athletId
          && wc.wertung.wettkampfdisziplinId === this.wertung.wettkampfdisziplinId
          && wc.wertung.endnote !== this.wertung.endnote) {
          this.item.wertung = Object.assign({}, wc.wertung);
          this.itemOriginal.wertung = Object.assign({}, wc.wertung);
          this.wertung = Object.assign({
            noteD: 0.00,
            noteE: 0.00,
            endnote: 0.00
          }, this.item.wertung);
        }
      });
    });
    this.platform.ready().then(() => {
      // We need to use a timeout in order to set the focus on load
      setTimeout(() => {
        let Keyboard;
        if (Capacitor.getPlatform() !== "web") {
          if (Keyboard.show) {
            Keyboard.show(); // Needed for android. Call in a platform ready function
            console.log('keyboard called');
          }
        }
        const dnote = this.dnote();
        const enote = this.enote();
        if (this.isDNoteUsed && dnote) {
          dnote.setFocus();
          console.log('dnote focused');
        } else if (enote) {
          enote.setFocus();
          console.log('enote focused');
        }
      }, 400);

    });

  }

  editable() {
    return this.backendService.loggedIn && this.wertung.wettkampfdisziplinId > 0;
  }

  updateExercices() {
    const scvs = this.groupBy([...this.wertung.variables?.dVariables || [], ...this.wertung.variables?.eVariables || [], ...this.wertung.variables?.pVariables || []], v => v.index);
    this.exercices = Object.keys(scvs).map(k => scvs[k]).sort((a, b) => a[0].index - b[0].index);
  }

  updateUI(wc: WertungContainer) {

    this.zone.run(() => {
      this.mediaStateChanged(this.backendService.mediaStateChanged.value);
      this.waiting = false;
      this.item = Object.assign({}, wc);
      this.isDNoteUsed = this.item.isDNoteUsed;
      this.itemOriginal = Object.assign({}, wc);
      this.wertung = Object.assign({
        noteD: 0.00,
        noteE: 0.00,
        endnote: 0.00
      }, this.itemOriginal.wertung);

      const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === wc.wertung.id);
      let nextItemIndex = currentItemIndex + 1;
      if (currentItemIndex < 0 || currentItemIndex >= this.backendService.wertungen.length - 1) {
        this.nextItem = undefined;
      } else {
        this.nextItem = this.backendService.wertungen[nextItemIndex];
      }

      this.ionViewWillEnter();
    });
  }

  ensureInitialValues(wertung: Wertung): Wertung {
    return Object.assign(this.wertung, wertung);
  }


  mediaStateChanged(message: AthletMediaIsAtStart|AthletMediaIsRunning|AthletMediaIsPaused|AthletMediaIsFree) {
    if (this.wertung?.mediafile === null || this.geraetName() !== 'Boden') {
      this.currentMediaState = 'keine Musik zugeordnet';
      this.currentMediaButtonDisabled = true;
      this.currentMediaReleaseButtonDisabled = true;
      this.currentMediaButtonText = '';
    } else {
      switch (message.type) {
        case 'AthletMediaIsFree':
          this.currentMediaState = 'Musik Player ist frei';
          this.currentMediaButtonText = 'Musik laden';
          this.currentMediaButtonIcon = 'recording-outline';
          this.currentMediaAction = this.loadMusic;
          this.currentMediaButtonDisabled = false;
          this.currentMediaReleaseButtonDisabled = true;
          break;
        default:
          if (message.media?.id === this.wertung?.mediafile?.id) {
            this.currentMediaReleaseButtonDisabled = false;
            switch (message.type) {
              case 'AthletMediaIsAtStart':
                this.currentMediaState = 'Musik ist bereit für ' + message.context;
                this.currentMediaButtonText = 'Musik abspielen';
                this.currentMediaButtonIcon = 'play-circle-outline';
                this.currentMediaAction = this.playMusic;
                this.currentMediaButtonDisabled = false;
                break;
              case 'AthletMediaIsRunning':
                this.currentMediaState = 'Musik läuft für ' + message.context;
                this.currentMediaButtonText = 'Musik stoppen';
                this.currentMediaButtonIcon = 'stop-circle-outline';
                this.currentMediaAction = this.playMusic;
                this.currentMediaButtonDisabled = false;
                break;
              case 'AthletMediaIsPaused':
                this.currentMediaState = 'Musik ist pausiert für ' + message.context;
                this.currentMediaButtonText = 'Musik abspielen';
                this.currentMediaButtonIcon = 'play-circle-outline';
                this.currentMediaAction = this.playMusic;
                this.currentMediaButtonDisabled = false;
                break;
              case 'AthletMediaIsFree':
                this.currentMediaState = 'Musik Player ist frei';
                this.currentMediaButtonText = 'Musik abspielen';
                this.currentMediaButtonIcon = 'play-circle-outline';
                this.currentMediaAction = this.playMusic;
                this.currentMediaButtonDisabled = false;
                break;
            }
          } else {
            this.currentMediaState = 'Musik Player ist belegt für ' + message.context;
            this.currentMediaButtonText = 'Musik laden';
            this.currentMediaButtonIcon = 'recording-outline';
            this.currentMediaAction = () => {};
            this.currentMediaButtonDisabled = true;
            this.currentMediaReleaseButtonDisabled = true;
          }
      }
    }
  }

  loadMusic() {
    this.backendService.aquireMusic(this.ensureInitialValues(this.wertung));
  }
  releaseMusic() {
    this.backendService.releaseMusic(this.ensureInitialValues(this.wertung));
  }
  playMusic() {
    const message = this.backendService.mediaStateChanged.value;
    const type = message.type;
    switch (type) {
      case 'AthletMediaIsAtStart':
      case 'AthletMediaIsPaused':
        if (message.media.id === this.wertung.mediafile?.id) {
          this.backendService.playMusic(this.ensureInitialValues(this.wertung));
        }
        break;

      case 'AthletMediaIsRunning':
        if (message.media.id === this.wertung.mediafile?.id) {
          this.backendService.stopMusic(this.ensureInitialValues(this.wertung));
        } 
       break;

      case 'AthletMediaIsFree':
        this.backendService.aquireMusic(this.ensureInitialValues(this.wertung));
        this.backendService.playMusic(this.ensureInitialValues(this.wertung));
        break;
    }

  }

  validate(form: NgForm) {
    this.calculateEndnote(this.ensureInitialValues(form.value));
  }

  saveClose(form: NgForm) {
    if(!form.valid) return;
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe({
      next: (wc) => {
        this.updateUI(wc);
        this.navCtrl.pop();
      },
      error: (err) => {
        this.waiting = false;
        console.log(err);
    }});
  }

  save(form: NgForm) {
    if(!form.valid) return;
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe({
      next: (wc) => {
        this.updateUI(wc);
      },
      error: (err) => {
        this.waiting = false;
        console.log(err);
    }});
  }

  saveNext(form: NgForm) {
    if(!form.valid) return;
    this.waiting = true;
    this.backendService.updateWertung(this.durchgang, this.step, this.geraetId, this.ensureInitialValues(form.value)).subscribe({
      next: (wc) => {
        this.waiting = false;
        const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === wc.wertung.id);
        if (currentItemIndex < 0) {
          console.log('unexpected wertung - id matches not with current wertung: ' + wc.wertung.id);
        }
        let nextItemIndex = currentItemIndex + 1;
        if (currentItemIndex < 0) {
          nextItemIndex = 0;
        } else if (currentItemIndex >= this.backendService.wertungen.length - 1) {
          if (this.backendService.wertungen.filter(w => w.wertung.endnote === undefined).length === 0) {
            this.navCtrl.pop();
            this.toastSuggestCompletnessCheck();
            return;
          } else {
            nextItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.endnote === undefined);
            this.toastMissingResult(form, this.backendService.wertungen[nextItemIndex].vorname
              + ' ' + this.backendService.wertungen[nextItemIndex].name);
          }
        }
        //form.resetForm();
        this.updateUI(this.backendService.wertungen[nextItemIndex]);
      },
      error: (err) => {
        this.waiting = false;
        console.log(err);
    }});
  }

  itemTapped(slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 10 || amount < -10) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  next(form: NgForm, slidingItem: IonItemSliding) {
    slidingItem.close();
    const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === this.wertung.id);
    if (currentItemIndex < 0) {
      console.log('unexpected wertung - id matches not with current wertung: ' + this.wertung.id);
    }
    let nextItemIndex = currentItemIndex + 1;
    if (currentItemIndex < 0) {
      nextItemIndex = 0;
    } else if (currentItemIndex >= this.backendService.wertungen.length - 1) {
      if (this.backendService.wertungen.filter(w => w.wertung.endnote === undefined).length === 0) {
        this.navCtrl.pop();
        this.toastSuggestCompletnessCheck();
        return;
      } else {
        nextItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.endnote === undefined);
        this.toastMissingResult(form, this.backendService.wertungen[nextItemIndex].vorname
          + ' ' + this.backendService.wertungen[nextItemIndex].name);
      }
    }
    this.updateUI(this.backendService.wertungen[nextItemIndex]);
  }

  nextEmptyOrFinish(form: NgForm) {
    const currentItemIndex = this.backendService.wertungen.findIndex(w => w.wertung.id === this.wertung.id);
    if (this.backendService.wertungen.filter((w, index) => {
      return index > currentItemIndex && w.wertung.endnote === undefined;
    }).length === 0) {
      this.navCtrl.pop();
      this.toastSuggestCompletnessCheck();
      return;
    } else {
      const nextItemIndex = this.backendService.wertungen.findIndex((w, index) => {
        return index > currentItemIndex && w.wertung.endnote === undefined;
      });
      this.toastMissingResult(form, this.backendService.wertungen[nextItemIndex].vorname
        + ' ' + this.backendService.wertungen[nextItemIndex].name);
      //form.resetForm();
      this.updateUI(this.backendService.wertungen[nextItemIndex]);
    }
  }

  geraetName(): string {
    return this.backendService.geraete ? this.backendService.geraete.find(g => g.id === this.geraetId).name : '';
  }

  async toastSuggestCompletnessCheck() {
    const toast = await this.toastController.create({
      header: 'Qualitätskontrolle',
      message: 'Es sind jetzt alle Resultate erfasst. Bitte ZU ZWEIT die Resultate prüfen und abschliessen!',
      animated: true,
      position: 'middle',
      buttons: [
        {
          text: 'OK, gelesen.',
          icon: 'checkmark-circle',
          role: 'cancel',
          handler: () => {
            console.log('OK, gelesen. clicked');
          }
        }
      ]
    });
    toast.present();
  }

  async toastMissingResult(form: NgForm, athlet: string) {
    const alert = await this.alertCtrl.create({
      header: 'Achtung',
      subHeader: 'Fehlendes Resultat!',
      message: 'Nach der Erfassung der letzten Wertung in dieser Riege scheint es noch leere Wertungen zu geben. '
                + 'Bitte prüfen, ob ' + athlet.toUpperCase() + ' geturnt hat.',
      buttons:  [
        {
          text: 'Nicht geturnt',
          role: 'edit',
          handler: () => {
            this.nextEmptyOrFinish(form);
          }
        },
        {
          text: 'Korrigieren',
          role: 'cancel',
          handler: () => {
            console.log('Korrigieren clicked');
          }
        }
      ]
    });
    alert.present();
  }
}
