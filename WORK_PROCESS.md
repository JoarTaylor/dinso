# Hur du undersöker och förstår ett befintligt system.

Går igenom lite översiktligt flödet frontend till backend då jag inte skrivit så mycket Java.  
Så lite kul där för mig personligen.

För att få en helhetsbild av befintlig funktionalitet bad jag Claude göra lite excalidraw filer med:

> "i want the simple flow of the actions and the endpoints available, the overall logic of this app. Focus on the roles system"

Resultat i `dinso-roles-flow.excalidraw`.

# Hur du delar upp uppgiften och implementationen.

## steg 1: Planera och bestäm arkitektur.

prompt:

> "so the plan is to switch from a "role" based system to a more granular "permission" based system, these are the permissions: now, make me a plan and visualize in excalidraw how this would work. Note: we keep some roles, since the SYSTEM_ADMIN role should be able to tweak a demo users permissions. Make me a plan, and make an excalidraw for this new permission based system "

bestämde mig för en "per-company" modell, för användare kan rimligen ha olika permissions per anställning

vi har kvar 2 roller som vi behöver

```java
public enum DemoRole {
PRIVATE_CUSTOMER,
SYSTEM_ADMIN
}
```

När denna logik är på plats så behöver vi GUI'n för att lägga upp behörigheter.

För att stödja mer skalbar ui körde jag denna prompt:

> "i want to improve the UI where the system_admin changes
> permissions, we should be able to easily change the  
>  permissions of 100 employees and
> 100 different permissions for example"

sen promptade jag fram att vi ska kunna sätta "presets" på permissions, dvs de enkla förval som "chef" etc

märkte av en bug att statet på användarna uppdateras inte när system-admin ändra behörigheter - fixat

gjorde hanteringen av behörigheter mer skalbar, med en switch för att välja vilket företag

nästa steg är att jag vill att vardera medarbetare ska kunna se vilka behörigheter dem har, för transparans och lättare debugging

> "i want each person who has companies to see their own permissions in the specific company mode"

märker att på många ställen så kan man "försöka" göra grejer man inte har behörighet till

> "look over the actions in the client where the permissions allow  
> actions or not, some places should just be disabled because the  
> user lacks permissions, for example "lägg till medarbetare" goes  
> to "översikt" is the user lacks salary permission

> do an audit"

# Vad jag hade ändrat ifall jag hade mer tid

    1. Hämtat profiler från backend, just nu hårdkodat i frontend
    2. Ha flera portaler på en användare
    3. Vad är "läs information"? Antar nu att man ej nu kan läsa "dokument" om man inte har läsbehörighet
        Lösning: om man ej har read på en arbetsgivare, denna ska bli hidden för användaren.
        och read på minst 1 för att ens kunna logga in, annars dold "välj portal" och "logga in"
        nu gör jag detta hidden för användaren, ett val jag gör helt enkelt

    claude skapade en "bulk" permission, men gällar för alla företag, vi vill ha "per company" så jag tar bort denna
    4. FIXA MOBIL- fixat permissions toggle viewn iaf
    5.


    la till en skill för frontend, om mer tid även lägga till en skill för backend

# Hur du går tillväga när du avgör vilken lösning som passar uppgiften bäst.

- Motivera dina val av lösningar till uppgifterna.

# Hur du avgör att lösningen är klar för granskning.
