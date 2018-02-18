import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { backendUrl } from './utils';
import { TokenInterceptor } from './token-interceptor';
import { toBase64String } from '@angular/compiler/src/output/source_map';
import { WertungContainer, Geraet, Wettkampf, Wertung } from './backend-types';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { tokenNotExpired } from 'angular2-jwt';

declare var location: any;

@Injectable()
export class BackendService {
  loggedIn = false;

  competitions: Wettkampf[];
  durchgaenge: string[];
  geraete: Geraet[];
  steps: string[];
  wertungen: WertungContainer[];

  constructor(public http: HttpClient) {
    this.loggedIn = this.checkJWT();
  }

  private checkJWT(): boolean {
    const token = localStorage.getItem('auth_token');
    return !!token && tokenNotExpired(token);
  }

  login(username, password) {
    const headers = new HttpHeaders();
    this.http.options(backendUrl + 'api/login', { 
      observe: 'response', 
      headers: headers.set('Authorization', 'Basic ' + btoa(username + ':' + password)),
      withCredentials: true,
      responseType: 'text'
    }).subscribe((data) => {
      console.log(data);
      localStorage.setItem('auth_token', data.headers.get('x-access-token'));
      this.loggedIn = this.checkJWT();
    }, (err) => {
      console.log(err);
      localStorage.removeItem('auth_token');
      this.loggedIn = this.checkJWT();
    });
  }

  logout() {
    localStorage.removeItem('auth_token');
    this.loggedIn = this.checkJWT();
  }

  getCompetitions() {
    this.http.get<Wettkampf[]>(backendUrl + 'api/competition').subscribe((data) => {
      this.competitions = data;
    });
  }

  getDurchgaenge(competitionId: string) {
    this.durchgaenge = undefined;
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<string[]>(backendUrl + 'api/competition/' + competitionId).subscribe((data) => {
      this.durchgaenge = data;
    });
  }

  getGeraete(competitionId: string, durchgang: string) {
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<Geraet[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang).subscribe((data) => {
      this.geraete = data;
    });
  }

  getSteps(competitionId: string, durchgang: string, geraetId: number) {
    this.steps = undefined;
    this.wertungen = undefined;
    this.http.get<string[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang + '/' + geraetId).subscribe((data) => {
      this.steps = data;
    });
  }

  getWertungen(competitionId: string, durchgang: string, geraetId: number, step: number) {
    this.wertungen = undefined;
    this.http.get<WertungContainer[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang + '/' + geraetId + '/' + step).subscribe((data) => {
      this.wertungen = data;
    });
  }

  updateWertung(durchgang: string, step: number, geraetId: number, wertung: Wertung): Observable<WertungContainer> {
    const competitionId = wertung.wettkampfUUID;
    const result = new Subject<WertungContainer>();
    this.http.put<WertungContainer[]>(backendUrl + 'api/competition/' + competitionId + '/' + durchgang + '/' + geraetId + '/' + step, wertung)
    .subscribe((data) => {
      this.wertungen = data;
      result.next(data.find(wc => wc.wertung.id === wertung.id));
      result.complete();
    }, (err)=> {
      result.next(this.wertungen.find(wc => wc.wertung.id === wertung.id));
      result.error(err);
    });    
    return result;
  }
}