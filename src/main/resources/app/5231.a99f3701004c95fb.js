"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[5231],{5231:(k,c,l)=>{l.r(c),l.d(c,{RegAthletEditorPageModule:()=>C});var h=l(6895),d=l(433),u=l(5472),o=l(502),_=l(7225),e=l(8274),m=l(600);function p(r,s){if(1&r&&(e.TgZ(0,"ion-select-option",15)(1,"ion-avatar",2),e._UZ(2,"img",8),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e._UZ(5,"br"),e.TgZ(6,"small"),e._uU(7),e.ALo(8,"date"),e.qZA()()()),2&r){const t=s.$implicit;e.Q6J("value",t.athletId),e.xp6(4),e.lnq("",t.name,", ",t.vorname," (",t.geschlecht,")"),e.xp6(3),e.hij("(",e.lcZ(8,5,t.gebdat),")")}}function b(r,s){if(1&r){const t=e.EpF();e.TgZ(0,"ion-select",19),e.NdJ("ngModelChange",function(n){e.CHM(t);const a=e.oxw(2);return e.KtG(a.selectedClubAthletId=n)}),e.YNc(1,p,9,7,"ion-select-option",20),e.qZA()}if(2&r){const t=e.oxw(2);e.Q6J("ngModel",t.selectedClubAthletId),e.xp6(1),e.Q6J("ngForOf",t.clubAthletList)}}function A(r,s){1&r&&(e.TgZ(0,"ion-item-divider")(1,"ion-avatar",0),e._UZ(2,"img",21),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4,"Einteilung"),e.qZA()())}function f(r,s){if(1&r&&(e.TgZ(0,"ion-select-option",15),e._uU(1),e.qZA()),2&r){const t=s.$implicit;e.Q6J("value",t.id),e.xp6(1),e.Oqu(t.name)}}function v(r,s){if(1&r){const t=e.EpF();e.TgZ(0,"ion-item")(1,"ion-avatar",0),e._uU(2," \xa0 "),e.qZA(),e.TgZ(3,"ion-label",12),e._uU(4,"Programm/Kategorie"),e.qZA(),e.TgZ(5,"ion-select",22),e.NdJ("ngModelChange",function(n){e.CHM(t);const a=e.oxw(2);return e.KtG(a.registration.programId=n)}),e.YNc(6,f,2,2,"ion-select-option",20),e.qZA()()}if(2&r){const t=e.oxw(2);e.xp6(5),e.Q6J("ngModel",t.registration.programId),e.xp6(1),e.Q6J("ngForOf",t.filterPGMsForAthlet(t.registration))}}function I(r,s){if(1&r){const t=e.EpF();e.TgZ(0,"ion-content")(1,"form",6,7),e.NdJ("ngSubmit",function(){e.CHM(t);const n=e.MAs(2),a=e.oxw();return e.KtG(a.save(n))})("keyup.enter",function(){e.CHM(t);const n=e.MAs(2),a=e.oxw();return e.KtG(a.save(n))}),e.TgZ(3,"ion-list")(4,"ion-item-divider")(5,"ion-avatar",0),e._UZ(6,"img",8),e.qZA(),e.TgZ(7,"ion-label"),e._uU(8,"Athlet / Athletin"),e.qZA(),e.YNc(9,b,2,2,"ion-select",9),e.qZA(),e.TgZ(10,"ion-item")(11,"ion-avatar",0),e._uU(12," \xa0 "),e.qZA(),e.TgZ(13,"ion-input",10),e.NdJ("ngModelChange",function(n){e.CHM(t);const a=e.oxw();return e.KtG(a.registration.name=n)}),e.qZA(),e.TgZ(14,"ion-input",11),e.NdJ("ngModelChange",function(n){e.CHM(t);const a=e.oxw();return e.KtG(a.registration.vorname=n)}),e.qZA()(),e.TgZ(15,"ion-item")(16,"ion-avatar",0),e._uU(17," \xa0 "),e.qZA(),e.TgZ(18,"ion-label",12),e._uU(19,"Geburtsdatum"),e.qZA(),e.TgZ(20,"ion-input",13),e.NdJ("ngModelChange",function(n){e.CHM(t);const a=e.oxw();return e.KtG(a.registration.gebdat=n)}),e.qZA()(),e.TgZ(21,"ion-item")(22,"ion-avatar",0),e._uU(23," \xa0 "),e.qZA(),e.TgZ(24,"ion-label",12),e._uU(25,"Geschlecht"),e.qZA(),e.TgZ(26,"ion-select",14),e.NdJ("ngModelChange",function(n){e.CHM(t);const a=e.oxw();return e.KtG(a.registration.geschlecht=n)}),e.TgZ(27,"ion-select-option",15),e._uU(28,"weiblich"),e.qZA(),e.TgZ(29,"ion-select-option",15),e._uU(30,"m\xe4nnlich"),e.qZA()()(),e.YNc(31,A,5,0,"ion-item-divider",4),e.YNc(32,v,7,2,"ion-item",4),e.qZA(),e.TgZ(33,"ion-list")(34,"ion-button",16,17),e._UZ(36,"ion-icon",18),e._uU(37,"Speichern"),e.qZA()()()()}if(2&r){const t=e.MAs(2),i=e.oxw();e.xp6(9),e.Q6J("ngIf",0===i.registration.id&&i.clubAthletList.length>0),e.xp6(4),e.Q6J("disabled",!1)("ngModel",i.registration.name),e.xp6(1),e.Q6J("disabled",!1)("ngModel",i.registration.vorname),e.xp6(6),e.Q6J("disabled",!1)("ngModel",i.registration.gebdat),e.xp6(6),e.Q6J("ngModel",i.registration.geschlecht),e.xp6(1),e.Q6J("value","W"),e.xp6(2),e.Q6J("value","M"),e.xp6(2),e.Q6J("ngIf",i.needsPGMChoice()),e.xp6(1),e.Q6J("ngIf",i.needsPGMChoice()),e.xp6(2),e.Q6J("disabled",i.waiting||!i.isFormValid()||!t.valid||t.untouched)}}function Z(r,s){if(1&r){const t=e.EpF();e.TgZ(0,"ion-button",23,24),e.NdJ("click",function(){e.CHM(t);const n=e.oxw();return e.KtG(n.delete())}),e._UZ(2,"ion-icon",25),e._uU(3,"Anmeldung l\xf6schen"),e.qZA()}if(2&r){const t=e.oxw();e.Q6J("disabled",t.waiting)}}const x=[{path:"",component:(()=>{class r{constructor(t,i,n,a,g){this.navCtrl=t,this.route=i,this.backendService=n,this.alertCtrl=a,this.zone=g,this.waiting=!1}ngOnInit(){this.waiting=!0,this.wkId=this.route.snapshot.paramMap.get("wkId"),this.regId=parseInt(this.route.snapshot.paramMap.get("regId")),this.athletId=parseInt(this.route.snapshot.paramMap.get("athletId")),this.backendService.getCompetitions().subscribe(t=>{const i=t.find(n=>n.uuid===this.wkId);this.wettkampfId=parseInt(i.id),this.backendService.loadProgramsForCompetition(i.uuid).subscribe(n=>{this.wkPgms=n,this.backendService.loadAthletListForClub(this.wkId,this.regId).subscribe(a=>{this.clubAthletList=a,this.athletId?this.backendService.loadAthletRegistrations(this.wkId,this.regId).subscribe(g=>{this.clubAthletListCurrent=g,this.updateUI(g.find(M=>M.id===this.athletId))}):this.updateUI({id:0,vereinregistrationId:this.regId,name:"",vorname:"",geschlecht:"W",gebdat:void 0,programId:void 0,registrationTime:0})})})})}get selectedClubAthletId(){return this._selectedClubAthletId}set selectedClubAthletId(t){this._selectedClubAthletId=t,this.registration=this.clubAthletList.find(i=>i.athletId===t)}needsPGMChoice(){const t=[...this.wkPgms][0];return!(1==t.aggregate&&2==t.riegenmode)}alter(t){const i=new Date(t.gebdat).getFullYear();return new Date(Date.now()).getFullYear()-i}similarRegistration(t,i){return t.athletId===i.athletId||t.name===i.name&&t.vorname===i.vorname&&t.gebdat===i.gebdat&&t.geschlecht===i.geschlecht}alternatives(t){return this.clubAthletListCurrent?.filter(i=>this.similarRegistration(i,t)&&i.id!=t.id)||[]}filterPGMsForAthlet(t){const i=this.alter(t),n=this.alternatives(t);return this.wkPgms.filter(a=>(a.alterVon||0)<=i&&(a.alterBis||100)>=i&&0===n.filter(g=>g.programId===a.id).length)}editable(){return this.backendService.loggedIn}updateUI(t){this.zone.run(()=>{this.waiting=!1,this.wettkampf=this.backendService.competitionName,this.registration=Object.assign({},t),this.registration.gebdat=(0,_.tC)(this.registration.gebdat)})}isFormValid(){return!this.registration?.programId&&!this.needsPGMChoice()&&(this.registration.programId=this.filterPGMsForAthlet(this.registration)[0]?.id),!!this.registration.gebdat&&!!this.registration.geschlecht&&this.registration.geschlecht.length>0&&!!this.registration.name&&this.registration.name.length>0&&!!this.registration.vorname&&this.registration.vorname.length>0&&!!this.registration.programId&&this.registration.programId>0}checkPersonOverride(t){if(t.athletId){const i=[...this.clubAthletListCurrent,...this.clubAthletList].find(n=>n.athletId===t.athletId);if(i.geschlecht!==t.geschlecht||new Date((0,_.tC)(i.gebdat)).toJSON()!==new Date(t.gebdat).toJSON()||i.name!==t.name||i.vorname!==t.vorname)return!0}return!1}save(t){if(!t.valid)return;const i=Object.assign({},this.registration,{gebdat:new Date(t.value.gebdat).toJSON()});0===this.athletId||0===i.id?(this.needsPGMChoice()||this.filterPGMsForAthlet(this.registration).filter(n=>n.id!==i.programId).forEach(n=>{this.backendService.createAthletRegistration(this.wkId,this.regId,Object.assign({},i,{programId:n.id}))}),this.backendService.createAthletRegistration(this.wkId,this.regId,i).subscribe(()=>{this.navCtrl.pop()})):this.checkPersonOverride(i)?this.alertCtrl.create({header:"Achtung",subHeader:"Person \xfcberschreiben vs korrigieren",message:"Es wurden \xc4nderungen an den Personen-Feldern vorgenommen. Diese sind ausschliesslich f\xfcr Korrekturen zul\xe4ssig. Die Identit\xe4t der Person darf dadurch nicht ge\xe4ndert werden!",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"Korektur durchf\xfchren",handler:()=>{this.backendService.saveAthletRegistration(this.wkId,this.regId,i).subscribe(()=>{this.navCtrl.pop()})}}]}).then(g=>g.present()):this.backendService.saveAthletRegistration(this.wkId,this.regId,i).subscribe(()=>{this.navCtrl.pop()})}delete(){this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+this.registration.name+", "+this.registration.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.needsPGMChoice()||this.clubAthletListCurrent.filter(i=>this.similarRegistration(this.registration,i)).filter(i=>i.id!==this.registration.id).forEach(i=>{this.backendService.deleteAthletRegistration(this.wkId,this.regId,i)}),this.backendService.deleteAthletRegistration(this.wkId,this.regId,this.registration).subscribe(()=>{this.navCtrl.pop()})}}]}).then(i=>i.present())}}return r.\u0275fac=function(t){return new(t||r)(e.Y36(o.SH),e.Y36(u.gz),e.Y36(m.v),e.Y36(o.Br),e.Y36(e.R0b))},r.\u0275cmp=e.Xpm({type:r,selectors:[["app-reg-athlet-editor"]],decls:14,vars:3,consts:[["slot","start"],["defaultHref","/"],["slot","end"],[1,"athlet"],[4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],[3,"ngSubmit","keyup.enter"],["athletRegistrationForm","ngForm"],["src","assets/imgs/athlete.png"],["placeholder","Auswahl aus Liste","name","clubAthletid","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["required","","placeholder","Name","type","text","name","name","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Vorname","type","text","name","vorname","required","",3,"disabled","ngModel","ngModelChange"],["color","primary"],["required","","placeholder","Geburtsdatum","type","date","display-timezone","utc","name","gebdat","required","",3,"disabled","ngModel","ngModelChange"],["required","","placeholder","Geschlecht","name","geschlecht","okText","Okay","cancelText","Abbrechen","required","",3,"ngModel","ngModelChange"],[3,"value"],["size","large","expand","block","type","submit","color","success",3,"disabled"],["btnSaveNext",""],["slot","start","name",""],["placeholder","Auswahl aus Liste","name","clubAthletid","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],["src","assets/imgs/wettkampf.png"],["required","","placeholder","Programm/Kategorie","name","programId","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],["size","large","expand","block","color","danger",3,"disabled","click"],["btnDelete",""],["slot","start","name","trash"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title"),e._uU(5,"Anmeldung Athlet/Athletin"),e.qZA(),e.TgZ(6,"ion-note",2)(7,"div",3),e._uU(8),e.qZA()()()(),e.YNc(9,I,38,13,"ion-content",4),e.TgZ(10,"ion-footer")(11,"ion-toolbar")(12,"ion-list"),e.YNc(13,Z,4,1,"ion-button",5),e.qZA()()()),2&t&&(e.xp6(8),e.hij("f\xfcr ",i.wettkampf,""),e.xp6(1),e.Q6J("ngIf",i.registration),e.xp6(4),e.Q6J("ngIf",i.athletId>0))},dependencies:[h.sg,h.O5,d._Y,d.JJ,d.JL,d.Q7,d.On,d.F,o.BJ,o.oU,o.YG,o.Sm,o.W2,o.fr,o.Gu,o.gu,o.pK,o.Ie,o.rH,o.Q$,o.q_,o.uN,o.t9,o.n0,o.wd,o.sr,o.QI,o.j9,o.cs,h.uU]}),r})()}];let C=(()=>{class r{}return r.\u0275fac=function(t){return new(t||r)},r.\u0275mod=e.oAB({type:r}),r.\u0275inj=e.cJS({imports:[h.ez,d.u5,o.Pc,u.Bz.forChild(x)]}),r})()}}]);