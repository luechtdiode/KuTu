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

export interface NewLastResults {
  results: WertungContainer[];
  lastTopResults: WertungContainer[];
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
  noteD:number;
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
  athlet: any, 
  wertung: Wertung; 
  wettkampfUUID: string;
  durchgang: string;
  geraet: number;
  type: string;
}