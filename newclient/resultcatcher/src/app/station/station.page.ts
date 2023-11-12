import { Component, OnInit, ChangeDetectionStrategy, NgZone } from '@angular/core';
import { NavController, AlertController, IonItemSliding } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { encodeURIComponent2 } from '../services/websocket.service';
import { Wettkampf, Geraet, WertungContainer } from '../backend-types';
import { map, tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { gearMapping } from '../utils';

@Component({
  selector: 'app-station',
  templateUrl: './station.page.html',
  styleUrls: ['./station.page.scss']
})
export class StationPage implements OnInit  {

  durchgangopen = false;
  geraete: Geraet[] = [];

  constructor(public navCtrl: NavController, public backendService: BackendService,
              private alertCtrl: AlertController, private zone: NgZone) {
    this.backendService.durchgangStarted.pipe(
      map(dgl =>
      dgl.filter(dg =>
        encodeURIComponent2(dg.durchgang) === encodeURIComponent2(this.backendService.durchgang)
        && dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    )).subscribe(dg => {
      this.durchgangopen = dg;
    });
  }

  ionViewWillEnter() {
    if (this.geraete.length > 0 && !this.backendService.captionmode) {
      this.backendService.activateCaptionMode();
    }
  }
  ngOnInit(): void {
    this.backendService.geraeteSubject.subscribe(geraete => {
      if (this.backendService.captionmode) {
        this.geraete = geraete;
      }
    });
  }

  durchgangstate() {
    const connected = this.backendService.isWebsocketConnected() ? (this.durchgangopen ? 'gestartet' : 'gesperrt') : 'offline';
    return connected;
  }
  get durchgangstateClass() {
    const classes = {
      open: this.backendService.isWebsocketConnected() && this.durchgangopen,
      closed: this.backendService.isWebsocketConnected() && !this.durchgangopen,
      offline: !this.backendService.isWebsocketConnected()
    };
    return classes;
  }

  set competition(competitionId: string) {
    if (!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
    }
  }
  get competition(): string {
    return this.backendService.competition;
  }

  set durchgang(d: string) {
    if (!this.stationFreezed) {
      this.backendService.getGeraete(this.competition, d);
    }
  }
  get durchgang(): string {
    return this.backendService.durchgang;
  }

  set geraet(geraetId: number) {
    if (!this.stationFreezed) {
      this.backendService.getSteps(this.competition, this.durchgang, geraetId);
    }
  }
  get geraet(): number {
    return this.backendService.geraet;
  }

  set step(s) {
    this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, s);
  }
  get step() {
    return this.backendService.step;
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }
  isLoggedIn() {
    return this.backendService.loggedIn;
  }

  get nextStepCaption(): string {
    if (this.isLoggedIn()) {
      return 'Nächste Riege';
    } else {
      return 'Nächstes Gerät';
    }
  }

  get prevStepCaption(): string {
    if (this.isLoggedIn()) {
      return 'Vorherige Riege';
    } else {
      return 'Vorheriges Gerät';
    }
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

  nextStep(slidingItem: IonItemSliding) {
    this.backendService.nextGeraet().subscribe(step => {
      this.step = step;
      slidingItem.close();
    });
  }

  prevStep(slidingItem: IonItemSliding) {
    this.backendService.prevGeraet().subscribe(step => {
      this.step = step;
      slidingItem.close();
    });
  }

  get station() {
    if (this.isLoggedIn()) {
      return this.geraetName() + ' - ' + this.step + '. Riege von ' + this.getSteps()?.length + ', ' + this.durchgang;
    } else {
      return this.geraetName() + ' - ' + this.step + '. Gerät von ' + this.getGeraete()?.length + ', ' + this.durchgang;
    }
  }
  
  get previousGearImage() {
    return this.isLoggedIn() ? undefined : gearMapping[this.backendService.getPrevGeraet()] || undefined;
  }

  get nextGearImage() {
    return this.isLoggedIn() ? undefined : gearMapping[this.backendService.getNextGeraet()] || undefined;
  }

  get gearImage() {
    return gearMapping[this.backendService.geraet] || undefined;
  }

  async showCompleteAlert() {
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      message: 'Nach dem Abschliessen der Station "'
                + this.station
                + '" können die erfassten Wertungen nur noch im Wettkampf-Büro korrigiert werden.',
      buttons: [
        {text: 'Abbrechen', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          const navTransition = alert.then(a => a.dismiss());
          this.zone.run(() => {
            this.backendService.finishStation(this.competition, this.durchgang, this.geraet, this.step)
            .subscribe(nextSteps => {
              if (nextSteps.length === 0) {
                navTransition.then(() => {
                  this.navCtrl.pop();
                });
              }
            });
          });
          return false;
        }
      },
    ]
    });
    alert.then(a => a.present());
  }

  async toastMissingResult(undefinedItems: WertungContainer[]) {
    const firstUndefinedAthlet = undefinedItems[0].vorname.toUpperCase() + ' '
      + undefinedItems[0].name.toUpperCase()
      + ' (' + undefinedItems[0].verein + ')';

    const alert = await this.alertCtrl.create({
      header: 'Achtung',
      subHeader: 'Fehlendes Resultat!',
      message: 'In dieser Riege gibt es noch ' + undefinedItems.length + ' leere Wertungen! - '
                + 'Bitte prüfen, ob ' + firstUndefinedAthlet + ' geturnt hat.',
      buttons:  [
        {text: 'Abbrechen', role: 'cancel', handler: () => {}},
        {
          text: firstUndefinedAthlet + ' hat nicht geturnt',
          role: 'edit',
          handler: () => {
            undefinedItems.splice(0, 1);
            if (undefinedItems.length > 0) {
              this.toastMissingResult(undefinedItems);
            } else {
              this.showCompleteAlert();
            }
          }
        },
        {
          text: 'Korrigieren',
          role: 'edit',
          handler: () => {
            this.navCtrl.navigateForward('wertung-editor/' + undefinedItems[0].id);
          }
        }
      ]
    });
    alert.present();
  }

  finish() {
    const undefinedItems = this.backendService.wertungen.filter(w => w.wertung.endnote === undefined);
    if (undefinedItems.length === 0) {
      this.showCompleteAlert();
      return;
    } else {
      this.toastMissingResult(undefinedItems);
    }
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }
  competitionName() {
    if (!this.backendService.competitions) { return ''; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + '<br>' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  geraetName(): string {
    if (!this.geraete || this.geraete.length === 0) { return ''; }
    const candidate = this.geraete
      .filter(c => c.id === this.backendService.geraet)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }
  getDurchgaenge(): string[] {
    return this.backendService.durchgaenge;
  }

  getGeraete(): Geraet[] {
    return this.geraete || [];
  }

  getSteps(): number[] {
    // if (! this.backendService.steps && this.backendService.geraet) {
    //   this.backendService.getSteps(this.competition, this.durchgang, this.geraet);
    //   /*.subscribe(steps => {
    //     this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, 1);
    //   })
    //   */
    // }
    return this.backendService.steps;
  }

  getWertungen(): Observable<WertungContainer[]> {
    return this.backendService.wertungenSubject;
  }

}
