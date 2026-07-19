import { Component, OnInit, inject, ChangeDetectionStrategy } from '@angular/core';
import { StartList, Wettkampf, Teilnehmer, ProgrammItem } from '../backend-types';
import { NavController, IonItemSliding, AlertController, ToastController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { async } from 'rxjs/internal/scheduler/async';
import { BehaviorSubject, Subject, of, Observable } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';
import { debounceTime, distinctUntilChanged, map, filter, switchMap, tap, share } from 'rxjs/operators';
import { AdminBackendService } from '../services/admin-backend.service';
import { SecretService } from '../services/secret.service';

@Component({
    selector: 'app-search-athlet',
    templateUrl: './search-athlet.page.html',
    styleUrls: ['./search-athlet.page.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class SearchAthletPage implements OnInit {
  navCtrl = inject(NavController);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  backendService = inject(BackendService);
  private adminBackend = inject(AdminBackendService);
  private secretService = inject(SecretService);
  private alertCtrl = inject(AlertController);
  private toastCtrl = inject(ToastController);


  sStartList: StartList;
  sFilteredStartList: StartList;
  sMyQuery: string;
  adminPlaybookMode = false;
  private adminSecret = '';

  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;

  private busy = new BehaviorSubject(false);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);

  constructor() {
    if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  ngOnInit(): void {
    this.busy.next(true);
    const wkId = this.route.snapshot.paramMap.get('wkId');
    this.sMyQuery = this.route.snapshot.queryParamMap.get('query') || '';
    this.adminPlaybookMode = this.route.snapshot.queryParamMap.get('admin') === 'true'
      && this.route.snapshot.queryParamMap.get('origin') === 'playbook';
    if (wkId) {
      if (this.adminPlaybookMode) {
        const storedSecret = this.secretService.getSecret(wkId);
        this.adminSecret = storedSecret?.secret || '';
        this.adminPlaybookMode = this.adminSecret.trim().length > 0;
      }
      this.competition = wkId;
    }
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }

  set competition(competitionId: string) {
    this.syncPath(this.sMyQuery);
    if (!this.startlist || competitionId !== this.backendService.competition) {
      this.busy.next(true);
      this.startlist = {} as StartList;
      this.backendService.getDurchgaenge(competitionId);
      this.backendService.loadStartlist(undefined).subscribe(startlist => {
        this.busy.next(false);
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          //filter(event => !!event && !!event.target && !!event.target.value),
          map(event => event?.target?.value || ''),
          debounceTime(1000),
          distinctUntilChanged(),
          share()
        );
        pipeBeforeAction.subscribe(startSearchWith => {
          this.busy.next(true);
        });
        pipeBeforeAction.pipe(
          switchMap(this.runQuery(startlist))
        ).subscribe(filteredList => {
          this.sFilteredStartList = filteredList;
          this.busy.next(false);
          this.syncPath(this.sMyQuery)
        });
        this.startlist = startlist;
      });
    }
  }

  get competition(): string {
    return this.backendService.competition || '';
  }
  set startlist(startlist: StartList) {
    this.sStartList = startlist;
    let event = {target: {value: this.sMyQuery}};
    this.reloadList(event);
  }
  get startlist() {
    return this.sStartList;
  }
  get filteredStartList() {
    return this.sFilteredStartList || {
      programme : []
    } as StartList;
  }

  get isBusy(): Observable<boolean> {
    return this.busy;
  }

  syncPath(query: string = this.sMyQuery) {
    if (query?.trim().length > 0) {
      this.router.navigate(
        ['search-athlet', this.competition],
        {
          queryParams: {query: query}, // add query params to current url
          queryParamsHandling: 'merge', // remove to replace all query params by provided
        }
      );
    } else {
      this.router.navigate(['search-athlet', this.competition]);
    }
  }

  runQuery(startlist: StartList) {
    return (query: string) => {
      const q = query.trim();
      let result: StartList;

      result = {
        programme : []
      } as StartList;

      if (startlist) {

        startlist.programme.forEach(programm => {
          const filterFn = this.filter(q);
          const tnFiltered = programm.teilnehmer.filter(tn => filterFn(tn, programm.programm));
          if (tnFiltered.length > 0) {
            const pgItem = {
              programm : programm.programm,
              teilnehmer: tnFiltered
            } as ProgrammItem;
            result = Object.assign({}, startlist, {
              programme : [...result.programme, pgItem]
            });
          }
        });
      }

      return of(result);
    };
  }

  reloadList(event: any) {
    this.tMyQueryStream.next(event);
  }

  itemTapped(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 0) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  followAthlet(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.close();
    this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${item.athletid}`);
  }

  followRiege(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.close();
    this.backendService.getGeraete(this.backendService.competition,
      item.durchgang).subscribe(geraete => {
      this.backendService.getSteps(this.backendService.competition,
        item.durchgang, geraete.find(gg => gg.name === item.start).id).subscribe(steps => {
        this.navCtrl.navigateForward('station');
      });
    });
  }

  canUnassignFromCompetition(item: Teilnehmer): boolean {
    return this.adminPlaybookMode && this.adminSecret.trim().length > 0 && item.athletid > 0;
  }

  async unassignFromCompetition(item: Teilnehmer, slidingItem: IonItemSliding) {
    slidingItem.close();
    if (!this.canUnassignFromCompetition(item)) {
      return;
    }
    const alert = await this.alertCtrl.create({
      header: 'Athlet aus Wettkampf entfernen',
      message: `${item.athlet} wird aus der Wettkampf-Einteilung entfernt.`,
      buttons: [
        { text: 'Abbrechen', role: 'cancel' },
        {
          text: 'Entfernen',
          role: 'destructive',
          handler: () => {
            this.adminBackend.unassignAthletFromCompetition(this.competition, item.athletid, this.adminSecret).subscribe({
              next: async (result) => {
                const removed = result?.removedWertungen || 0;
                await this.showToast(`Athlet aus Wettkampf entfernt (${removed} Wertungen).`, 'success');
                this.refreshStartList();
              },
              error: async () => {
                await this.showToast('Fehler beim Entfernen des Athleten.', 'danger');
              }
            });
          }
        }
      ]
    });
    await alert.present();
  }

  private refreshStartList() {
    this.busy.next(true);
    this.backendService.loadStartlist(undefined).subscribe({
      next: startlist => {
        this.busy.next(false);
        this.startlist = startlist;
      },
      error: () => {
        this.busy.next(false);
      }
    });
  }

  private async showToast(message: string, color: 'success' | 'danger') {
    const toast = await this.toastCtrl.create({
      message,
      duration: 2500,
      color
    });
    await toast.present();
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions || [];
  }
  competitionName(): string {
    if (!this.backendService.competitions) { return ''; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

    if (candidate.length === 1) {
      if (!this.startlist) {
        this.competition = this.backendService.competition;
      }
      return candidate[0];
    } else {
      return '';
    }
  }

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: Teilnehmer, pgm: string): boolean => {
      return queryTokens.filter(token => {
        if (tn.athletid + '' === token) {
          return true;
        }
        if (tn.athlet.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.verein.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.start.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.verein.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.team.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (pgm.indexOf(token) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }
}
