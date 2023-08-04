export interface WertungContainer {
  id: number;
  vorname: string;
  name: string;
  geschlecht: string;
  verein: string;
  wertung: Wertung;
  geraet: number;
  programm: string;
  isDNoteUsed: boolean;
}

export interface BulkEvent {
  events: any[];
  wettkampfUUID: string;
}
export interface NewLastResults {
  results: {string: WertungContainer};
  lastTopResults: {string: WertungContainer};
}

export interface Teilnehmer {
  athlet: string;
  athletid: number;
  durchgang: string;
  start: string;
  verein: string;
}

export interface ProgrammItem {
  programm: string;
  teilnehmer: Teilnehmer[];
}

export interface StartList {
  logo: string;
  title: string;
  programme: ProgrammItem[];
}

export interface ProgrammRaw {
  id: number;
  name: string;
  aggregate: number;
  parentId: number;
  ord: number;
  alterVon: number;
  alterBis: number;
  uuid: string;
  riegenmode: number;
}

export interface Verein {
  id: number;
  name: string;
  verband: string;
}
export interface ClubRegistration {
  id: number;
  mail: string;
  mobilephone: string;
  registrationTime: number;
  respName: string;
  respVorname: string;
  verband: string;
  vereinId?: number;
  vereinname: string;
  wettkampfId: number;
}

export interface NewClubRegistration {
  mail: string;
  mobilephone: string;
  respName: string;
  respVorname: string;
  verband: string;
  vereinname: string;
  wettkampfId: number;
  secret: string;
  verification?: string;
}

export interface RegistrationResetPW {
  id: number;
  wettkampfId: number;
  secret: string;
  verification?: string;
}
export interface AthletRegistration {
  id: number;
  vereinregistrationId: number;
  athletId?: number;
  name: string;
  vorname: string;
  geschlecht: string;
  gebdat: string;
  programId: number;
  team: number;
  registrationTime: number;
}

export interface JudgeRegistration {
  id: number;
  vereinregistrationId: number;
  geschlecht: string;
  name: string;
  vorname: string;
  mobilephone: string;
  mail: string;
  comment: string;
  registrationTime: number;
}
export interface JudgeRegistrationProgramItem {
  program: string;
  disziplin: string;
  disziplinId: number;
}
 export interface SyncAction {
   caption: string;
   verein: ClubRegistration;
 }
export interface Wertung {
  id: number;
  wettkampfId: number;
  wettkampfUUID: string;
  wettkampfdisziplinId: number;
  athletId: number;
  riege: string;
  endnote: number;
  noteE: number;
  noteD: number;
  team: number;
}

export interface Geraet {
  id: number;
  name: string;
}

export interface Wettkampf {
  id: string;
  programmId: number;
  datum: Date;
  titel: string;
  auszeichnung: number;
  auszeichnungendnote: number;
  uuid: string;
  notificationEMail?: string;
  altersklassen?: string;
  jahrgangsklassen?: string;
  punktegleichstandsregel?: string;
  rotation?: string;
  teamrule?: string;
}

export interface ScoreRow {
  "athletID"?: number,
  "rows"?: ScoreRow[];
  "Athlet"?: string,
  "Team/Athlet"?: string,
  "K"?: string,
  "Jahrgang"?: string,
  "Verein"?: string,
  "Rang": string,
  "ø Gerät": string,
  "Total Punkte": string
}

export interface ScoreBlockTitle {
  level: number;
  text: string;
}
export interface ScoreBlock {
  title: ScoreBlockTitle;
  rows: ScoreRow[];
}
export interface Score {
  logo?: string;
  title?: string;
  scoreblocks?: ScoreBlock[];
}

export interface ScoreLink {
  name?: string;
  published?: boolean;
  "published-date"?: string;
  "scores-href"?: string;
  "scores-query"?: string;
}

// actions
export interface FinishDurchgangStation {
  wettkampfUUID: string;
  durchgang: string;
  geraet: number;
  step: number;
}

// websocket events
export interface MessageAck {
  msg: string;
  type: string;
}

export interface DurchgangStarted {
  wettkampfUUID: string;
  durchgang: string;
  type: string;
}
export interface DurchgangFinished {
  wettkampfUUID: string;
  durchgang: string;
  type: string;
}
export interface AthletWertungUpdated {
  athlet: any;
  wertung: Wertung;
  wettkampfUUID: string;
  durchgang: string;
  geraet: number;
  type: string;
  sequenceId: number;
}
